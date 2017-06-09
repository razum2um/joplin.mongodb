# joplin.mongodb

```
[joplin.mongodb "0.1.0"]
```

## Usage

Write joplin config with `:type :mongodb :uri "..." :collection "..."`

```
{:databases
  {:mongodb
    {:type :mongodb
     :uri "mongodb://userb71148a:0da0a696f23a4ce1ecf6d11382633eb2049d728e@cluster1.mongohost.com:27034/app81766662"
     :collections "collection to store migration ids, `migrations` by default"}}}

```

Generate migrations

```
(joplin.repl/create config :mongodb "migartion-name")
```

Use [monger](https://github.com/michaelklishin/monger) and `as-db->` inside your migrations:

```
(ns migrations.mongodb.20170609050347-example
  (:require [joplin.mongodb.database :refer :all]
            [monger.collection :as mc]))

(defn up [db]
  (as-db-> db mongodb (mc/update mongodb "notifications" {} {:$rename {:environment :env}} {:multi true})))

(defn down [db]
  (as-db-> db mongodb (mc/update mongodb "notifications" {} {:$rename {:env :environment}} {:multi true})))
```

## License

Copyright © 2017 Vlad Bokov

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
