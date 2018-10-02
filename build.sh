#!/bin/bash -e
#time lein uberjar && time ~/bin/s3cp.sh target/reagent-helloworld.jar

if [ "$SKIP_BUILD" = "" ]; then
  lein clean && lein cljsbuild once min && lein minify-assets
fi

mkdir -p release

cat > release/index.html << EOF
<html><head><meta charset="utf-8"><meta content="width=device-width, initial-scale=1" name="viewport">
<link href="site.min.css" rel="stylesheet" type="text/css"></head>
<body class="body-container">
  <div id="app"><h3>ClojureScript has not been compiled!</h3></div>
  <script src="app.js" type="text/javascript"></script>
</body></html>
EOF

cp target/cljsbuild/public/js/app.js* resources/public/css/site.min.css src/cljs/rubikstimer_clj/core.cljs release

D=nikonyrh-public/misc/rubiktimer-cljs
echo "https://s3-eu-west-1.amazonaws.com/$D/index.html"
aws s3 sync release "s3://$D" --acl public-read

