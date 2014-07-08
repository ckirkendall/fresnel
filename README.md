# Fresnel (formerly known as Segments) 

A library for composing lenses and working with complex state objects.  This library is a paired down version lens code embeded in Christophe Grand's Eliven. 


## Artifact

All artifacts are published to [clojars](https://clojars.org/segments). Latest version is `0.1.0`:

[![Clojars Project](http://clojars.org/fresnel/latest-version.svg)](http://clojars.org/fresnel)

## Concepts
 
The concept of a lens is very simple.  A lens consist of two method; one that transforms the state object into desired form and one that takes a transform object and pushes its state back into the main state object.  Fresnel provides a mechanism to define lenses and to compose them.  Fesnel builds on the concepts that aready existing in clojure.  In clojure we see two types of lenses for associative structures.  The first is the keyword and we use get and assoc as the transform functions.  The other is composition of these lenses in the form of a vector.  We use get-in and assoc-in as transform functions in this case.  Fresnel extends this concept beyond associative structions and unifies the access mentods under a single protocol Lens.

```clj
(def state {:a {:aa "a1,b1,c1"
                :ab 2}
            :b {:ba "a2,b2,c2"
                :bb 2}})

;fresnel supports custom lenses
(deflens comma-to-map [oval nval]
  :fetch 
    (reduce #(assoc %1 (.trim %2) true) {} (split oval #","))
  :putback
    (reduce #(if %1 (str %2 "," %1) %2)
            nil
            (filter #(nval %1) (keys nval))))

;fresnel supports composing of lenses
(def compound-lens [:a :aa comma-to-map])

(fetch state compound-lens)
;{:a1 true :b1 true :c1 true}

(def new-state 
   (putback state 
            compound-lens 
            {:a1 false :b1 true :c1 true :d1 true}))

;;new-state {:a {:aa "b1,c1,d1" :ab 2}
;;           :b {:ba "a2,b2,c2" :bb 2}})     


```


## License

Copyright © 2014 Creighton Kirkendall

Distributed under the Eclipse Public License either version 1.0 or (at
your option) any later version.
