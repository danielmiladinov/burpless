(ns burpless
  (:require [burpless.runtime :refer [create-cucumber-runtime]]))

(defmacro step
  "Create a step map, with line and file filled in.

   - `kw`: :Given, :When or :Then
   - `pattern`: The `CucumberExpression` / `RegularExpression` to match for this step
   - `step-fn`: The function to call when executing this step.
                Output parameters (`CucumberExpression`) / subgroups matched in `pattern` (`RegularExpression`)
                are provided as parameters."
  [kw pattern step-fn]
  (let [line (:line (meta &form))]
    `{:glue-type :step
      :kw        ~kw
      :pattern   ~pattern
      :function  ~(vary-meta step-fn #(select-keys % [:datatable :docstring]))
      :line      ~line
      :file      ~*file*}))


(defmacro hook
  "Create a hook map"
  [phase hook-fn]
  (let [line (:line (meta &form))]
    `{:glue-type :hook
      :phase     ~phase
      :order     0
      :function  ~hook-fn
      :line      ~line
      :file      ~*file*}))

(defmacro parameter-type
  "Create a parameter-type map"
  [{:keys [name regexps to-type transform use-for-snippets? prefer-for-regexp? strong-type-hint?]
    :or   {use-for-snippets?  true
           prefer-for-regexp? true
           strong-type-hint?  true}}]
  (let [line (:line (meta &form))]
    `{:glue-type          :parameter-type
      :name               ~name
      :regexps            ~regexps
      :to-type            ~to-type
      :transform          ~transform
      :use-for-snippets?  ~use-for-snippets?
      :prefer-for-regexp? ~prefer-for-regexp?
      :strong-type-hint?  ~strong-type-hint?
      :line               ~line
      :file               ~*file*}))

(defmacro datatable-type
  "Create a datatable-type map.
  The following keys are required:
  - :from-type -> one of the following keywords: :table, :row, :entry, :cell
  - :to-type   -> the return type of the transform function, an instance of java.lang.reflect.Type
  - :transform -> a function that is expected to receive an argument of one of the following types:
                  :table -> DataTable  (the entire datatable)
                  :row   -> List<String> (a list of cells in a datatable row)
                  :entry -> Map<String, String> (a map of column names to column values), one for each row
                  :cell  -> String (a single datable cell)

                  The input argument corresponds to the value of :from-type
                  The transform fn should return a value of :to-type"

  [{:keys [to-type
           from-type
           transform]}]
  (let [line (:line (meta &form))]
    `{:glue-type :datatable-type
      :to-type   ~to-type
      :from-type ~from-type
      :transform ~transform
      :line      ~line
      :file      ~*file*}))

(defmacro docstring-type
  "Create a docstring-type-map.
  The following keys are required:
  - :content-type -> the GFM info string or media type that decorates the docstrings you wish to transform
  - :to-type      -> the return type of the transform function, an instance of java.lang.reflect.Type
  - :transform    -> a function that is expected to receive the string content of the docstring, and transform it into
                     the type represented by :to-type."
  [{:keys [content-type
           to-type
           transform]}]
  (let [line (:line (meta &form))]
    `{:glue-type    :docstring-type
      :content-type ~content-type
      :to-type      ~to-type
      :transform    ~transform
      :line         ~line
      :file         ~*file*}))

(defn run-cucumber
  "Run the cucumber features at `features-path` using the given `glues`.

  `glues` should be a sequence of glue definition maps - glues can be one of the following:
  - steps
  - hooks
  - parameter types
  - datatable types
  - docstring types

  There are macros for each of the above, which let you easily create glues of the desired type.

  Defaults to using the pretty plugin with monochrome disabled.
  Feel free to call passing different args to suit your needs if desired.
  For a (hopefully) complete list of supported plugins,
  see also: https://github.com/cucumber/cucumber-jvm/blob/main/cucumber-core/src/main/resources/io/cucumber/core/options/USAGE.txt

  Returns a byte representing the exit code of the test.
  Zero indicates test success; non-zero values imply something went wrong."
  ([x y]
   (run-cucumber x y {:plugin "pretty"}))
  ([features-path glues args]
   (let [state-atom (atom nil)
         runtime    (create-cucumber-runtime (assoc args :features-path features-path) glues state-atom)]
     (.run runtime)
     (.exitStatus runtime))))
