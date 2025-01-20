(ns burpless.runtime
  (:require [clojure.edn :as edn]
            [clojure.string :as str])
  (:import (clojure.lang Atom IObj Keyword Symbol)
           (io.cucumber.core.backend Backend DataTableTypeDefinition DocStringTypeDefinition Glue HookDefinition ParameterInfo ParameterTypeDefinition Snippet StaticHookDefinition StepDefinition TestCaseState TypeResolver)
           (io.cucumber.core.gherkin DataTableArgument DocStringArgument Feature Pickle)
           (io.cucumber.core.gherkin.messages GherkinMessagesFeatureParser)
           (io.cucumber.core.options CommandlineOptionsParser CucumberProperties CucumberPropertiesParser)
           (io.cucumber.core.runtime BackendSupplier FeatureSupplier)
           (io.cucumber.cucumberexpressions CucumberExpression ExpressionFactory GroupBuilder ParameterType ParameterTypeRegistry RegularExpression Transformer)
           (io.cucumber.datatable DataTable DataTableType TableCellTransformer TableEntryTransformer TableRowTransformer TableTransformer)
           (io.cucumber.docstring DocString DocStringType DocStringType$Transformer)
           (java.io File)
           (java.lang.reflect Field Method ParameterizedType Type)
           (java.net URI)
           (java.nio.file Files OpenOption Path)
           (java.text MessageFormat)
           (java.util List Locale Map)))

(defn- access-private-field
  "Given an object instance and a symbol referring to one of its non-public fields,
  return that field's value."
  [^Object instance ^Symbol field-name]
  (-> (doto ^Field (->> (-> instance .getClass .getDeclaredFields)
                        (filter #(-> % .getName (.equals (name field-name))))
                        first)
        (.setAccessible true))
      (.get instance)))

(defn- invoke-private-method
  "Given an object instance (or a Class) and a symbol referring to one of its non-public methods,
  invoke that method with any provided args."
  [^Object instance-or-class ^Symbol method-name & args]
  (let [^Class clazz (if (class? instance-or-class) instance-or-class (class instance-or-class))
        method-args  (to-array args)]
    (-> (doto ^Method (->> (.getDeclaredMethods clazz)
                           (filter #(-> % .getName (.equals (name method-name))))
                           first)
          (.setAccessible true))
        (.invoke instance-or-class method-args))))

(defn- to-parameter-info
  "Return a ParameterInfo for a given Type, or java.lang.Object, if none was given."
  (^ParameterInfo [] (to-parameter-info Object))

  (^ParameterInfo [^Type type]
   (reify ParameterInfo
     (^Type getType [_] type)
     (^boolean isTransposed [_] false)
     (^TypeResolver getTypeResolver [_]
       (reify TypeResolver
         (^Type resolve [_] type))))))

(defmulti ^:private to-parameter-infos
          "There's more than one way to produce a list of ParameterInfo objects from a StepExpression,
          and we decide which method to use by dispatching on the type of the incoming StepExpression.

          For CucumberExpressions, the work is much simpler as we can determine the parameter info directly from
          the parameter types embedded in the expression string. See cucumber-expression-parameter-types below for
          more detail.

          For RegularExpressions, we chose to dig into Cucumber-JVM's non-public APIs to leverage its capabilities
          for extracting parameter types from regular expression capture groups.
          Although ParameterTypeRegistry itself is a public API of the Cucumber Expressions library, and we could have
          theoretically just instantiated a registry instance of our own, we wonder whether the instance held by
          a RegularExpression instance might have been further augmented with additional parameter types
          after initialization via calls to its defineParameterType method.
          See also: https://github.com/cucumber/cucumber-expressions/blob/main/java/src/main/java/io/cucumber/cucumberexpressions/ParameterTypeRegistry.java"
          (comp type :expression))

;; Given a CucumberExpression, get at its source property, which is the actual string which we expect to contain zero
;; or more embedded parameter type expressions.
;; Escaped curly braces (see https://github.com/cucumber/cucumber-expressions/tree/main#escaping) will be ignored.
;; Unrecognized parameter types (any not found in the cucumber-expression-parameter-types map above) will need special
;; treatment.
;; The defmethod macro doesn't allow for docstrings or else this comment block would have been a docstring :-/
(defmethod ^:private to-parameter-infos CucumberExpression
  [{:keys [^CucumberExpression expression
           ^ParameterTypeRegistry registry]}]
  (let [parameter-types (->> (invoke-private-method registry 'getParameterTypes)
                             (reduce (fn [m p]
                                       (assoc m (format "{%s}" (.getName p))
                                                (.getType p)))
                                     {}))]
    (or (->> (re-seq #"((?<!\\)\{.*?\})" (.getSource expression))
             (mapv (comp to-parameter-info parameter-types second)))
        [])))

(defmethod ^:private to-parameter-infos RegularExpression
  [{:keys [^RegularExpression expression
           ^ParameterTypeRegistry registry]}]
  (let [capture-groups (-> (access-private-field expression 'treeRegexp)
                           (access-private-field 'groupBuilder)
                           (access-private-field 'groupBuilders))]
    (mapv (fn [^GroupBuilder capture-group]
            (let [source         (invoke-private-method capture-group 'getSource)
                  parameter-type (invoke-private-method registry 'lookupByRegexp source (.getRegexp expression) "")
                  inner-type     (or (some-> parameter-type (.getType)) String)]
              (to-parameter-info inner-type)))
          capture-groups)))

(defn- to-datatable-parameter-info
  "Given a map of fn metadata known to contain a :datatable key, based on its value,
  return the appropriate ParameterInfo instance."
  (^ParameterInfo [{:keys [datatable] :as fn-metadata}]
   (to-parameter-info
     (cond (nil? datatable) DataTable
           (true? datatable) DataTable
           (instance? Type datatable) datatable
           (and (vector? datatable)
                (instance? Type (first datatable)))
           (reify ParameterizedType
             ;; Type hint for Java Type array (i.e. Type[] in Java)
             (^"[Ljava.lang.reflect.Type;" getActualTypeArguments [_]
               (into-array Type datatable))
             (^Type getRawType [_]
               List)
             (^Type getOwnerType [_]
               nil))
           :else
           (throw (ex-info (str/join "\n" ["Unexpected step fn metadata - :datable value should either be:"
                                           "- boolean true,"
                                           "- a Type instance, or"
                                           "- a vector containing a Type instance"])
                           fn-metadata))))))

(defn- to-docstring-parameter-info
  "Given a map of fn metadata known to contain a :docstring key, based on its value,
  return the appropriate ParameterInfo instance."
  (^ParameterInfo [{:keys [docstring] :as fn-metadata}]
   (to-parameter-info
     (cond (nil? docstring) DocString
           (true? docstring) DocString
           (instance? Type docstring) docstring
           :else
           (throw (ex-info (str/join "\n" ["Unexpected step fn metadata - :datable value should either be:"
                                           "- boolean true, or"
                                           "- a Type instance"])
                           fn-metadata))))))

(defn- find-step-argument
  "Given a step expression, the list of parameter infos for that expression, and a sequence pickle step candidates,
  return the argument of the pickle step whose text corresponds to the expression.
  This is necessary for determining whether a step needs an extra argument for either a datatable or docstring."
  [expression step-parameter-infos pickle-steps]
  (let [types (into-array Type (map ParameterInfo/.getType step-parameter-infos))]
    (some->> pickle-steps
             not-empty
             (filter (comp some? (fn [text] (.match expression text types)) :text))
             first
             :argument)))

(defn- to-step-definition
  "Given a ParameterTypeRegistry, an ExpressionFactory, a sequence of pickle steps,
  a sequence of DocStringTypeDefinitions, a map describing the step definition to be built, and a state atom,
  return a StepDefinition implementation, which after execution its effects should be observable in the state atom."
  (^StepDefinition [^ParameterTypeRegistry registry
                    ^ExpressionFactory expression-factory
                    pickle-steps
                    docstring-typedefs
                    {:keys [pattern function file line]}
                    ^Atom state-atom]
   (let [pattern-str          (str pattern)
         expression           (.createExpression expression-factory pattern-str)
         fn-metadata          (meta function)
         expr-parameter-infos (to-parameter-infos {:expression expression :registry registry})
         pickle-step-argument (find-step-argument expression expr-parameter-infos pickle-steps)
         datatable?           (or (:datatable fn-metadata) (instance? DataTableArgument pickle-step-argument))
         docstring?           (or (:docstring fn-metadata) (instance? DocStringArgument pickle-step-argument))
         all-parameter-infos  (cond-> expr-parameter-infos
                                      datatable? (conj (to-datatable-parameter-info fn-metadata))
                                      docstring? (conj (to-docstring-parameter-info fn-metadata)))]
     (reify StepDefinition
       (^void execute [_ ^objects args]
         (let [array-length (alength args)
               last-idx     (dec array-length)
               last-arg     (when (<= 0 last-idx)
                              (aget args last-idx))]
           ;; Apply DocStringType transformations here ourselves, since it seems that cucumber-jvm isn't applying them
           ;; for us, as it seems to be doing with DatatableType transformations… how curious!
           (when (instance? DocString last-arg)
             (let [content-type (.getContentType ^DocString last-arg)
                   transform    (fn [content]
                                  (let [docstring-type (some->> docstring-typedefs
                                                                (map DocStringTypeDefinition/.docStringType)
                                                                (filter (comp (hash-set content-type)
                                                                              (fn [^DocStringType docstring-type]
                                                                                (invoke-private-method
                                                                                  docstring-type
                                                                                  'getContentType))))
                                                                first)]
                                    (if (some? docstring-type)
                                      (invoke-private-method docstring-type 'transform content)
                                      content)))]
               (aset args last-idx (-> ^DocString last-arg .getContent transform))))
           (apply swap! state-atom function args)))
       (^List parameterInfos [_]
         all-parameter-infos)
       (^String getPattern [_]
         pattern-str)
       (^boolean isDefinedAt [_ ^StackTraceElement element]
         (and (= line (.getLineNumber element))
              (= file (.getFileName element))))
       (^String getLocation [_]
         (str file ":" line))))))

(defn- to-hook-definition
  "Given a map describing the hook definition to be built, and a state atom,
  return a HookDefinition implementation which after execution its effects should be observable in the state atom."
  [{:keys [phase order function file line]} state-atom]
  (let [{:keys [tag]} (meta function)]
    (case phase
      (:before-all :after-all)
      (reify StaticHookDefinition
        (^void execute [_]
          (swap! state-atom function))
        (^int getOrder [_] order))
      (reify HookDefinition
        (^void execute [_ ^TestCaseState state]
          (swap! state-atom function state))
        (^String getTagExpression [_]
          (or tag ""))
        (^int getOrder [_]
          order)
        (^boolean isDefinedAt [_ ^StackTraceElement element]
          (and (= line (.getLineNumber element))
               (= file (.getFileName element))))
        (^String getLocation [_]
          (str file ":" line))))))

(defn- type-of
  "Return a representation of a given argument type.
  DataTables, IObjs, and Keywords get special treatment, in that we return the full name.
  For all other types, return the simple name."
  (^String [arg-type]
   (if (instance? Class arg-type)
     (if (#{DataTable IObj Keyword} arg-type)
       (.getName arg-type)
       (.getSimpleName arg-type))
     (str arg-type))))

(defn- to-parameter-type
  "Given a parameter-type descriptor map, return a ParameterType instance"
  (^ParameterType [{:keys [name regexps to-type transform
                           use-for-snippets? prefer-for-regexp? strong-type-hint?]}]
   (^[String List Type Transformer boolean boolean boolean]
     ParameterType/new ^String name
                       ^List (map str regexps)
                       ^Type to-type
                       ^Transformer transform
                       ^boolean use-for-snippets?
                       ^boolean prefer-for-regexp?
                       ^boolean strong-type-hint?)))

(defn- register-custom-parameter-type
  "Given a custom ParameterType, add it to both the provided glue and the parameter type registry.
  It must be added to both or else step functions will not be properly matched to gherkin steps at run time."
  [^Glue glue ^ParameterTypeRegistry parameter-type-registry ^ParameterType parameter-type]
  (.addParameterType glue (reify ParameterTypeDefinition (^ParameterType parameterType [_] parameter-type)))
  (.defineParameterType parameter-type-registry parameter-type))

(defmulti ^:private to-datatable-type
          "Given a datatable-type descriptor map, return a DataTableType instance"
          :from-type)

(defmethod ^:private to-datatable-type :table
  (^DataTableType [{:keys [to-type transform]}]
   (^[Type TableTransformer]
     DataTableType/new ^Type to-type
                       ^TableTransformer transform)))

(defmethod ^:private to-datatable-type :row
  (^DataTableType [{:keys [to-type transform]}]
   (^[Type TableRowTransformer]
     DataTableType/new ^Type to-type
                       ^TableRowTransformer transform)))

(defmethod ^:private to-datatable-type :entry
  (^DataTableType [{:keys [to-type transform]}]
   (^[Type TableEntryTransformer]
     DataTableType/new ^Type to-type
                       ^TableEntryTransformer transform)))

(defmethod ^:private to-datatable-type :cell
  (^DataTableType [{:keys [to-type transform]}]
   (^[Type TableCellTransformer]
     DataTableType/new ^Type to-type
                       ^TableCellTransformer transform)))

(defn- to-docstring-type
  "Given a docstring-type descriptor map, return a DocStringType instance"
  (^DocStringType [{:keys [content-type to-type transform]}]
   (^[Type String DocStringType$Transformer]
     DocStringType/new ^Type to-type
                       ^String content-type
                       ^DocStringType$Transformer transform)))

(defn- create-clojure-cucumber-backend
  "Given a parsed gherkin feature, a collection of glues, and a state atom,
  return a Clojure-friendly Cucumber Backend implementation."
  (^Backend [^Feature feature glues state-atom]
   (let [pickle-steps       (->> feature
                                 .getPickles
                                 (mapcat Pickle/.getSteps)
                                 (mapv (fn [step]
                                         {:keyword  (.getKeyword step)
                                          :type     (.getType step)
                                          :text     (.getText step)
                                          :argument (.getArgument step)})))
         {step-fns        :step
          hooks           :hook
          parameter-types :parameter-type
          datatable-types :datatable-type
          docstring-types :docstring-type} (group-by :glue-type glues)
         locale             (Locale/getDefault)
         registry           (ParameterTypeRegistry. locale)
         factory            (ExpressionFactory. registry)
         docstring-typedefs (atom nil)]
     (reify Backend
       (^void loadGlue [_ ^Glue glue ^List _gluePaths]
         (doseq [datatable-type (map to-datatable-type datatable-types)]
           (.addDataTableType glue (reify DataTableTypeDefinition (^DataTableType dataTableType [_] datatable-type))))
         (.addDocStringType glue (reify DocStringTypeDefinition
                                   (^DocStringType docStringType [_]
                                     (to-docstring-type {:content-type "edn"
                                                         :to-type      IObj
                                                         :transform    edn/read-string}))))
         (doseq [docstring-type (map to-docstring-type docstring-types)]
           (.addDocStringType glue (reify DocStringTypeDefinition (^DocStringType docStringType [_] docstring-type))))
         (register-custom-parameter-type
           glue
           registry
           (^[String List Type Transformer boolean boolean boolean]
             ParameterType/new "keyword"
                               ^List (map str [#":(\S+)"])
                               ^Type Keyword
                               ^Transformer keyword
                               true true true))
         (doseq [parameter-type (map to-parameter-type parameter-types)]
           (register-custom-parameter-type glue registry parameter-type))
         (doseq [{:keys [phase] :as hook} hooks
                 :let [hook-def (to-hook-definition hook state-atom)]]
           (case phase
             :before-all (.addBeforeAllHook glue hook-def)
             :after-all (.addAfterAllHook glue hook-def)
             :before (.addBeforeHook glue hook-def)
             :after (.addAfterHook glue hook-def)
             :before-step (.addBeforeStepHook glue hook-def)
             :after-step (.addAfterStepHook glue hook-def)))
         (let [typedefs (reset! docstring-typedefs (invoke-private-method glue 'getDocStringTypeDefinitions))]
           (doseq [step-fn step-fns]
             (.addStepDefinition glue (to-step-definition registry factory pickle-steps typedefs step-fn state-atom)))))
       (^void buildWorld [_])
       (^void disposeWorld [_])
       (^Snippet getSnippet [_]
         (let [pattern-last-escaped (atom nil)]
           (reify Snippet
             (^MessageFormat template [_]
               ;; {0} : Step Keyword</li>
               ;; {1} : Value of {@link #escapePattern(String)}</li>
               ;; {2} : Function name</li>
               ;; {3} : Value of {@link #arguments(Map)}</li>
               ;; {4} : Regexp hint comment</li>
               ;; {5} : value of {@link #tableHint()} if the step has a table</li>
               (MessageFormat.
                 (str/join "\n" ["(step :{0} \"{1}\""
                                 "      (fn [state{3}]"
                                 "        ;; {4}"
                                 "        (throw (io.cucumber.java.PendingException.))))"])))
             (^String tableHint [_] "")
             (^String arguments [_ ^Map arguments]
               ;; Relying on the fact that SnippetGenerator.createSnippet() calls escapePattern() with the value of the
               ;; pattern for which the snippet is being generated, before calling arguments()... :-/
               ;; If we remember what the pattern was that was just escaped, we can do some work in order to provide
               ;; a better type hint than just “String” for DocString types for which we know there is a transformation.
               ;; We do this by looking up our pickle steps for the one whose text matches the pattern
               ;; and checking its argument to see if it's a DocStringArgument.
               ;; DocStringTypes' transformations are organized by their (optional) media type
               ;; We've recorded a list of the defined DocStringType transformations - those provided to us by the step
               ;; fn author(s), and any others that might be hidden behind / built in to cucumber-jvm's glue
               ;; implementation
               (let [pattern         @pattern-last-escaped
                     expression      (.createExpression factory pattern)
                     parameter-infos (to-parameter-infos {:expression expression :registry registry})
                     argument        (find-step-argument expression parameter-infos pickle-steps)
                     mapped-types    (when (instance? DocStringArgument argument)
                                       (some->> @docstring-typedefs
                                                (map DocStringTypeDefinition/.docStringType)
                                                (reduce (fn [mappings docstring-type]
                                                          (assoc mappings
                                                            (invoke-private-method docstring-type 'getContentType)
                                                            (invoke-private-method docstring-type 'getType)))
                                                        {})))
                     args-str        (->> arguments
                                          (map (fn [[arg-name arg-type]]
                                                 (let [args (if (and (some? argument) (= "docString" arg-name))
                                                              (let [media-type (.getMediaType ^DocStringArgument argument)
                                                                    arg-type'  (get mapped-types media-type arg-type)
                                                                    arg-name'  (if (not= "String" arg-type')
                                                                                 "fromDocString"
                                                                                 "docString")]
                                                                [arg-type' arg-name'])
                                                              [arg-type arg-name])]
                                                   (apply format "^%s %s" (update args 0 type-of)))))
                                          (str/join " "))]
                 (cond->> args-str
                          (not (str/blank? args-str))
                          (str " "))))
             (^String escapePattern [_ ^String pattern]
               (reset! pattern-last-escaped pattern)
               (-> pattern
                   (str/replace "\\" "\\\\")
                   (str/replace "\"" "\\\""))))))))))

(def options
  "A map describing the runtime options that burpless supports, their options, and their default values.
  Derived from:
  https://github.com/cucumber/cucumber-jvm/blob/main/cucumber-core/src/main/resources/io/cucumber/core/options/USAGE.txt"
  {:threads        {:example "--threads COUNT"
                    :doc     "Number of threads to run tests under. Defaults to 1."
                    :default 1}
   :glue           {:example "-g, --glue PATH"
                    :short   :g
                    :doc     "Package to load glue code (step definitions, hooks and plugins)
                              from e.g: com.example.app. When not provided Cucumber will search the classpath."}
   :plugin         {:example "-p, --plugin PLUGIN[:[PATH|[URI [OPTIONS]]]"
                    :short   :p
                    :doc     "Register a plugin.
                              Built-in PLUGIN types:
                              html, json, junit, message, pretty, progress, rerun,
                              summary, teamcity, testng, timeline, usage, unused

                              PLUGIN can also be a fully qualified class name,
                              allowing registration of 3rd party plugins.
                              If a http:// or https:// URI is used, the output will be sent as a PUT request.
                              This can be overridden by providing additional options.

                              OPTIONS supports cUrls -X and -H commands."
                    :options [:html :json :junit :message :pretty :progress :rerun
                              :summary :teamcity :testng :timeline :usage :unused]}
   :tags           {:example "-t, --tags TAG_EXPRESSION"
                    :short   :t
                    :doc     "Only run scenarios tagged with tags matching TAG_EXPRESSION."}
   :name           {:example "-n, --name REGEXP"
                    :short   :n
                    :doc     "Only run scenarios whose names match REGEXP."}
   :dry-run        {:example  "-d, --[no-]dry-run"
                    :short    :d
                    :no-args? true
                    :no-?     true
                    :doc      "Skip execution of glue code."}
   :monochrome     {:example  "-m, --[no-]monochrome"
                    :short    :m
                    :no-args? true
                    :no-?     true
                    :doc      "Don't colour terminal output."}
   :snippets       {:example "--snippets [underscore|camelcase]"
                    :doc     "Naming convention for generated snippets. Defaults to underscore."
                    :options [:underscore :camelcase]
                    :default :underscore}
   :version        {:example  "-v, --version"
                    :short    :v
                    :no-args? true
                    :doc      "Print version."}
   :help           {:example  "-h, --help"
                    :short    :h
                    :no-args? true
                    :doc      "You're looking at it."}
   :i18n           {:example "--i18n LANG"
                    :doc     "List keywords for in a particular language
                              Run with '--i18n help' to see all languages"}
   :wip            {:example  "-w, --wip"
                    :short    :w
                    :no-args? true
                    :doc      "Fail if there are any passing scenarios."}
   :order          {:example "--order"
                    :doc     "Run the scenarios in a different order.
                              The options are 'reverse' and 'random'.
                              In case of 'random' order an optional
                              seed parameter can be added 'random:<seed>'."}
   :count          {:example "--count"
                    :doc     "Number of scenarios to be executed.
                              If not specified all scenarios are run."}
   :object-factory {:example "--object-factory CLASSNAME"
                    :doc     "Uses the class specified by CLASSNAME as object factory.
                              Be aware that the class is loaded through a service loader and therefore also
                              needs to be specified in: META-INF/services/io.cucumber.core.backend.ObjectFactory"}
   :uuid-generator {:example "--uuid-generator CLASSNAME"
                    :doc     "Uses the class specified by CLASSNAME as UUID generator.
                              Be aware that the class is loaded through a service loader and therefore
                              also needs to be specified in:
                              META-INF/services/io.cucumber.core.eventbus.UuidGenerator"}})

(defn to-cucumber-args
  "Given a map, translate the recognized key-value pairs into a string array of cucumber args."
  (^"[Ljava.lang.String;" [args-map]
   (let [{:keys [by-short-value invertible]}
         (reduce-kv (fn [m k {:keys [short no-?] :as v}]
                      (cond-> m
                              short (update :by-short-value assoc short v)
                              no-? (update :invertible assoc (keyword (str "no-" (name k))) v)))
                    {:by-short-value {}
                     :invertible     {}}
                    options)
         matching-options (juxt options by-short-value invertible)]
     (reduce-kv (fn [args k v]
                  (let [{:keys [short no-args? no-?] :as match} (first (remove nil? (matching-options k)))]
                    (cond-> args
                            (some? match)
                            (conj (->> k name (str (if (= short k) "-" "--"))))
                            (not (or no-? no-args?))
                            (conj (str v)))))
                []
                (into (sorted-map) args-map)))))

(defn create-cucumber-runtime
  "Given some args, glues, and a state atom, return a Clojure-friendly implementation of the Cucumber runtime."
  [{:keys [feature-path] :as args-map} glues state-atom]
  (let [cucumber-args           (into-array String (to-cucumber-args args-map))
        properties-file-options (-> (CucumberPropertiesParser.)
                                    (.parse (CucumberProperties/fromPropertiesFile))
                                    (.build))
        environment-options     (-> (CucumberPropertiesParser.)
                                    (.parse (CucumberProperties/fromEnvironment))
                                    (.build properties-file-options))
        system-options          (-> (CucumberPropertiesParser.)
                                    (.parse (CucumberProperties/fromSystemProperties))
                                    (.build environment-options))
        cli-options-parser      (CommandlineOptionsParser. System/out)
        runtime-options         (-> cli-options-parser
                                    (.parse cucumber-args)
                                    (.addDefaultGlueIfAbsent)
                                    (.addDefaultFeaturePathIfAbsent)
                                    (.addDefaultSummaryPrinterIfNotDisabled)
                                    (.enablePublishPlugin)
                                    (.build system-options))
        exit-status             (-> (.exitStatus cli-options-parser)
                                    (.orElse nil))]
    (or exit-status
        (let [class-loader   (.getContextClassLoader (Thread/currentThread))
              feature-uri    (URI. (str/join File/separator ["file:" File/separator (System/getProperty "user.dir") feature-path]))
              feature-stream (Files/newInputStream (Path/of feature-uri) (make-array OpenOption 0))
              feature        (-> (GherkinMessagesFeatureParser.)
                                 (.parse feature-uri feature-stream random-uuid)
                                 .get)
              backend        (create-clojure-cucumber-backend feature glues state-atom)
              runtime        (-> (io.cucumber.core.runtime.Runtime/builder) ;; disambiguated from java.lang.Runtime
                                 (.withRuntimeOptions runtime-options)
                                 (.withBackendSupplier (reify BackendSupplier (get [_] (vector backend))))
                                 (.withFeatureSupplier (reify FeatureSupplier (get [_] (vector feature))))
                                 (.withClassLoader (fn [] class-loader))
                                 (.build))]
          runtime))))
