# MongoTool
### Description
MongoTool allows you to do many things with MongoDB from your console with simple and easy commands to follow as of now we only allow you to import and export collections/databases, some people have problems with exporting/importing using atlas, sometimes with data getting corrupted (This has happened to us before) so we created this to give us ease of importing and exporting our databases to different servers or to local computers.

### Importing
To import you'll only need to provide a `clientURI` and `database` (Databases must be in the current directory of the jar to import, such as: `Gamer/Core/collections...`), all collections if using our export will be created in JSON, we require JSON to import and export because it's the easiest readable language.

### Exporting
To export you'' only need to provide a `clientURI` and `database` with the exception of `collection`, using the collection argument will allow you to only export that specific collection, we're soon to add an argument to exclude from the overall database collection list.
