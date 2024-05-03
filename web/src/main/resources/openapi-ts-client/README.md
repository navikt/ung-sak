This directory contains the generated **k9-sak.openapi.json** specification file and other files used as input to define
a generated openapi typescript client.

The k9-sak.openapi.json file is generated from the java source in this project, and represents a specification of the openapi the web server provides.

It is used to generate and publish a typescript/javascript client library npm package for communicating with the web server.

The k9-sak.openapi.json file is automatically generated during the build pipeline. Changes to the generated result is 
automatically committed to git in the build pipeline. One should never edit the file manually.

This is done so that the build pipeline can detect if there are changes in the api spec since last deployment, and only
publish a new package if there is changes. It also simplifies manual review of changes to the api spec during PR review.

The file is also intentionally git-ignored, so that if one runs the code generator locally changes should not be committed.


