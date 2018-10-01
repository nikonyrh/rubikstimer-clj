# rubikstimer-clj

This my first ClojureScript program, based on the [reagent-helloworld](https://github.com/reagent-project/reagent-template), which is a Leinegen template for projects using [reagent](https://github.com/reagent-project/reagent), which itself is a wrapper for [React](https://reactjs.org/).

I use this as a stopwatch when doing Rubiks cube solves, I am at the moment at 20 - 25 seconds with CFOP. The app is statically hosted at [AWS S3](https://s3-eu-west-1.amazonaws.com/nikonyrh-public/misc/rubiktimer-cljs/index.html), or you can easily run it locally. It won't record times less than two seconds, so if you screw up in the very beginning (or you are interrupted) you don't have to record the bad outcome.

It tries to be a bit responsive with the CSS layout so that it works with phones, tablets and PCs. On a PC you can use the spacebar for starting and stopping once you have clicked the focus there.

