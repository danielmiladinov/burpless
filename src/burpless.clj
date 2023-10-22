(ns burpless
  (:require [clojure.string :as str])
  (:import (clojure.lang Atom Symbol)
           (io.cucumber.core.backend Backend Glue HookDefinition ParameterInfo Snippet StaticHookDefinition StepDefinition TestCaseState TypeResolver)
           (io.cucumber.core.options CommandlineOptionsParser CucumberProperties CucumberPropertiesParser)
           (io.cucumber.core.runtime BackendSupplier)
           (io.cucumber.cucumberexpressions CucumberExpression ExpressionFactory GroupBuilder ParameterTypeRegistry RegularExpression)
           (io.cucumber.datatable DataTable)
           (io.cucumber.docstring DocString)
           (java.lang.reflect Field Method Type)
           (java.text MessageFormat)
           (java.util List Locale Map)
           (java.util.function Supplier)))

(def ^:dynamic *current-scenario-name* nil)

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
  "Return a ParameterInfo for a given Type, or java.lang.Class, if none was given."
  (^ParameterInfo [] (to-parameter-info Class))

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

          type)

(def ^:private cucumber-expression-parameter-types
  "A map of CucumberExpression parameter type strings keyed to the JVM types they map to.
  See also: https://github.com/cucumber/cucumber-expressions#parameter-types"
  {"{int}"        Integer
   "{float}"      Float
   "{word}"       String
   "{string}"     String
   "{}"           String
   "{bigdecimal}" BigDecimal
   "{double}"     Double
   "{biginteger}" BigInteger
   "{byte}"       Byte
   "{short}"      Short
   "{long}"       Long})

;; Given a CucumberExpression, get at its source property, which is the actual string which we expect to contain zero
;; or more embedded parameter type expressions.
;; Escaped curly braces (see https://github.com/cucumber/cucumber-expressions/tree/main#escaping) will be ignored.
;; Unrecognized parameter types (any not found in the cucumber-expression-parameter-types map above) will need special
;; treatment.
;; The defmethod macro doesn't allow for docstrings or else this comment block would have been a docstring :-/
(defmethod ^:private to-parameter-infos CucumberExpression
  [^CucumberExpression expression]
  (or (->> (re-seq #"((?<!\\)\{.*?\})" (.getSource expression))
           (mapv (comp to-parameter-info cucumber-expression-parameter-types second)))
      []))

(defmethod ^:private to-parameter-infos RegularExpression
  [^RegularExpression expression]
  (let [registry       (access-private-field expression 'parameterTypeRegistry)

        capture-groups (-> (access-private-field expression 'treeRegexp)
                           (access-private-field 'groupBuilder)
                           (access-private-field 'groupBuilders))]
    (mapv (fn [^GroupBuilder capture-group]
            (let [source         (invoke-private-method capture-group 'getSource)
                  parameter-type (invoke-private-method registry 'lookupByRegexp source (.getRegexp expression) "")
                  inner-type     (or (some-> parameter-type (.getType)) String)]
              (to-parameter-info inner-type)))
          capture-groups)))

(defn- to-step-definition
  "Given an ExpressionFactory, a map describing the step definition to be built, and a state atom,
  return a StepDefinition implementation which after execution its effects should be observable in the state atom."
  (^StepDefinition [^ExpressionFactory expression-factory
                    {:keys [pattern function file line]}
                    ^Atom state-atom]
   (let [pattern-str     (str pattern)
         fn-metadata     (meta function)
         expression      (.createExpression expression-factory pattern-str)
         parameter-infos (cond-> (to-parameter-infos expression)
                                 (:datatable fn-metadata) (conj (to-parameter-info DataTable))
                                 (:docstring fn-metadata) (conj (to-parameter-info DocString)))]
     (reify StepDefinition
       (^void execute [_ ^objects args]
         (apply swap! state-atom function args))

       (^List parameterInfos [_]
         parameter-infos)

       (^String getPattern [_]
         (str pattern))

       (^boolean isDefinedAt [_ ^StackTraceElement element]
         (and (= line (.getLineNumber element))
              (= file (.getFileName element))))

       (^String getLocation [_]
         (str file ":" line))))))

(defn- to-hook-definition
  "Given a map describing the hook definition to be built, and a state atom,
  return a HookDefinition implementation which after execution its effects should be observable in the state atom."
  [{:keys [phase order function]} state-atom]
  (case phase
    (:before-all :after-all) (reify StaticHookDefinition
                               (^void execute [_]
                                 (swap! state-atom function))

                               (^int getOrder [_] order))
    (reify HookDefinition
      (^void execute [_ ^TestCaseState state]
        (binding [*current-scenario-name* (.getName state)]
          (swap! state-atom function)))

      (^String getTagExpression [_]
        ; TODO feature tag expressions
        "")

      (^int getOrder [_] order))))

(defn- type-of
  "Return a representation of a given argument type.
  DataTables get special treatment, in that we return the full name.
  For all other types, return the simple name."
  (^String [arg-type]
   (if (instance? Class arg-type)
     (if (.equals arg-type DataTable)
       (.getName arg-type)
       (.getSimpleName arg-type))
     (str arg-type))))


(defn- create-clojure-cucumber-backend
  "Given a collection of step and/or hook descriptors, return a Clojure-friendly Cucumber Backend implementation."
  (^Backend [steps-or-hooks state-atom]
   (let [{steps :step hooks :hook} (group-by :type steps-or-hooks)]
     (reify Backend
       (^void loadGlue [_ ^Glue glue ^List _gluePaths]
         (let [registry           (ParameterTypeRegistry. (Locale/getDefault))
               expression-factory (ExpressionFactory. registry)]
           (doseq [{:keys [phase] :as hook} hooks
                   :let [hook-def (to-hook-definition hook state-atom)]]
             (case phase
               :before-all (.addBeforeAllHook glue hook-def)
               :after-all (.addAfterAllHook glue hook-def)
               :before (.addBeforeHook glue hook-def)
               :after (.addAfterHook glue hook-def)
               :before-step (.addBeforeStepHook glue hook-def)
               :after-step (.addAfterStepHook glue hook-def)))

           (doseq [step steps]
             (.addStepDefinition glue (to-step-definition expression-factory step state-atom)))))

       (^void buildWorld [_])
       (^void disposeWorld [_])

       (^Snippet getSnippet [_]
         (reify Snippet
           (^MessageFormat template [_]
             ;; {0} : Step Keyword</li>
             ;; {1} : Value of {@link #escapePattern(String)}</li>
             ;; {2} : Function name</li>
             ;; {3} : Value of {@link #arguments(Map)}</li>
             ;; {4} : Regexp hint comment</li>
             ;; {5} : value of {@link #tableHint()} if the step has a table</li>
             (MessageFormat.
               (str "(step :{0} \"{1}\"\n"
                    "      (fn {2} [state {3}]\n"
                    "        ;; {4}\n{5}"
                    "        (throw (io.cucumber.java.PendingException.))))")))

           (^String tableHint [_]
             (str/join "\n"
                       ["        ;; Be sure to also adorn your step function with the ^:datatable metadata"
                        "        ;; in order for the runtime to properly identify it and pass the datatable argument"
                        ""]))

           (^String arguments [_ ^Map arguments]
             (->> arguments
                  (map (fn [[arg-name arg-type]] (format "^%s %s" (type-of arg-type) arg-name)))
                  (str/join " ")))

           (^String escapePattern [_ ^String pattern]
             (-> pattern
                 (str/replace "\\" "\\\\")
                 (str/replace "\"" "\\\"")))))))))

(defn- create-cucumber-runtime
  "Given some args, steps / hooks, and a state atom, return a Clojure-friendly implementation of the Cucumber runtime."
  [args steps state-atom]
  (let [properties-file-options (-> (CucumberPropertiesParser.)
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
                                    (.parse (into-array String args))
                                    (.addDefaultGlueIfAbsent)
                                    (.addDefaultFeaturePathIfAbsent)
                                    (.addDefaultSummaryPrinterIfNotDisabled)
                                    (.enablePublishPlugin)
                                    (.build system-options))
        exit-status             (-> (.exitStatus cli-options-parser)
                                    (.orElse nil))]
    (or exit-status
        (let [backend (create-clojure-cucumber-backend steps state-atom)
              runtime (-> (io.cucumber.core.runtime.Runtime/builder) ;; disambiguated from java.lang.Runtime
                          (.withRuntimeOptions runtime-options)
                          (.withBackendSupplier (reify BackendSupplier (get [_] (vector backend))))
                          (.withClassLoader (reify Supplier (get [_] (.getContextClassLoader (Thread/currentThread)))))
                          (.build))]
          runtime))))

;;; Public api

(defmacro step
  "Create a step map, with line and file filled in.

   - `kw`: :Given, :When or :Then
   - `pattern`: The `CucumberExpression` / `RegularExpression` to match for this step
   - `step-fn`: The function to call when executing this step.
                Output parameters (`CucumberExpression`) / subgroups matched in `pattern` (`RegularExpression`)
                are provided as parameters."
  [kw pattern step-fn]
  (let [line (:line (meta &form))]
    `{:type     :step
      :kw       ~kw
      :pattern  ~pattern
      :function ~(vary-meta step-fn #(select-keys % [:datatable :docstring]))
      :line     ~line
      :file     ~*file*}))


(defmacro hook
  "Create a hook map"
  [phase hook-fn]
  (let [line (:line (meta &form))]
    `{:type     :hook
      :phase    ~phase
      :order    0
      :function ~hook-fn
      :line     ~line
      :file     ~*file*}))

(defn run-cucumber
  "Run the cucumber features at `features-path` using the given `steps`.

  `steps` should be a sequence of step definition maps; these can be
  created easily using the `step` macro.

  Defaults to using the pretty plugin with monochrome disabled.
  Feel free to call passing different args to suit your needs if desired.
  For a (hopefully) complete list of supported plugins,
  see also: https://github.com/cucumber/cucumber-jvm/blob/main/cucumber-core/src/main/resources/io/cucumber/core/options/USAGE.txt

  Returns a byte representing the exit code of the test.
  Zero indicates test success; non-zero values imply something went wrong."
  ([x y]
   (run-cucumber x y ["--plugin" "pretty"]))
  ([features-path steps args]
   (let [state-atom (atom nil)
         runtime    (create-cucumber-runtime (conj (vec args) features-path) steps state-atom)]
     (.run runtime)
     (.exitStatus runtime))))
