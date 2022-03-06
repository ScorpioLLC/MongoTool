package games.scorpio.mongo;

import com.google.gson.*;
import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import games.scorpio.mongo.util.FileUtil;
import joptsimple.OptionException;
import joptsimple.OptionParser;
import joptsimple.OptionSet;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class MongoTool {

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().serializeNulls().create();

    public static void main(String[] args) {
        OptionParser parser = new OptionParser();

        parser.accepts("clientURI").withRequiredArg().required().ofType(String.class);
        parser.accepts("import");
        parser.accepts("export");
        parser.accepts("excludes").withRequiredArg().required().ofType(String.class);
        parser.accepts("collection").withRequiredArg().ofType(String.class);
        parser.accepts("database").withRequiredArg().ofType(String.class);

        OptionSet options;

        try {
            options = parser.parse(args);
        } catch (OptionException ex) {
            System.out.println("Usage: MongoTool --clientURI <mongoUri> (--import <folder> | --export) (--collection <collectionName> | --database <databaseName>)");
            System.out.println(ex.getMessage());
            System.exit(1);
            return;
        }

        // Database is required everywhere (in exporting we allow single collections to be exported)
        if (!options.has("database")) {
            System.out.println("You must provide a database.");
            return;
        }

        MongoClient client;

        try {
            client = new MongoClient(new MongoClientURI((String) options.valueOf("clientURI")));
        } catch (Exception ex) {
            System.out.println("Invalid Client URI");
            System.exit(1);
            return;
        }

        if (options.has("import")) {
            importMongo(options, client, (String) options.valueOf("database"));
        } else if (options.has("export")) {
            export(options, client, (String) options.valueOf("database"), options.has("collection") ? (String) options.valueOf("collection") : null);
        } else {
            System.out.println("Unsupported method.");
            System.exit(1);
        }
    }

    public static void importMongo(OptionSet options, MongoClient client, String databaseName) {
        MongoDatabase database = client.getDatabase(databaseName);
        File folder = new File(databaseName);

        if (!folder.exists()) {
            System.out.println("No database in local directory named \"" + databaseName + "\".");
            System.exit(1);
            return;
        }

        if (folder.listFiles() == null) {
            System.out.println("Database must be a folder.");
            System.exit(1);
            return;
        }

        List<File> collections = new ArrayList<>();

        List<String> excludes = new ArrayList<>();

        // We allow people to exclude some collections from their database export
        if (options.has("excludes")) {
            excludes.addAll(Arrays.asList(((String) options.valueOf("excludes")).split(",")));
        }

        // Intellij really wants NonNull check here... when we check above
        // no idea, why Intellij is so weird.
        for (File collection : Objects.requireNonNull(folder.listFiles())) {
            if (!collection.getName().endsWith(".json")) {
                continue;
            }

            // We allow people to exclude when importing (Maybe don't wanna overwrite the current collection?)
            if (excludes.contains(collection.getName().split("\\.")[0])) {
                continue;
            }
            collections.add(collection);
        }

        long start = System.currentTimeMillis();

        for (File collection : collections) {
            FileUtil.read(collection, reader -> {
                List<Document> documents = new ArrayList<>();
                JsonArray array = gson.fromJson(reader, JsonArray.class);

                for (JsonElement element : array) {
                    documents.add(gson.fromJson(element, Document.class));
                }
                String collectionName = collection.getName().split("\\.")[0];
                database.getCollection(collectionName).insertMany(documents);
                System.out.println("Inserted " + documents.size() + " documents into " + collectionName);
            });
        }
        System.out.println("Completed import in " + (System.currentTimeMillis() - start) + "ms");
    }

    private static void export(OptionSet options, MongoClient client, String databaseName, String specificCollection) {
        List<String> collections = new ArrayList<>();

        MongoDatabase database = client.getDatabase(databaseName);

        // We use a list to go through all collections, if
        // we don't have a specific collection else we will
        // add the specific collection by itself to the collection
        if (specificCollection == null) {
            List<String> excludes = new ArrayList<>();

            // We allow people to exclude some collections from their database export
            if (options.has("excludes")) {
                excludes.addAll(Arrays.asList(((String) options.valueOf("excludes")).split(",")));
            }

            for (String collection : database.listCollectionNames()) {
                if (excludes.contains(collection)) {
                    continue;
                }
                collections.add(collection);
            }
        } else {
            collections.add(specificCollection);
        }

        File folder = new File(databaseName);

        if (!folder.exists()) {
            if (folder.mkdirs()) {
                System.out.println("Successfully created the folder for the database.");
            } else {
                System.out.println("Failed to create the folder for the database.");
                System.exit(1);
                return;
            }
        } else {
            System.out.println("Database already exists in current directory.");
        }

        long start = System.currentTimeMillis();

        for (String collectionName : collections) {
            try {
                File collectionFile = new File(folder, collectionName + ".json");

                if (!collectionFile.exists()) {
                    if (collectionFile.createNewFile()) {
                        System.out.println("Created " + collectionFile.getName());
                    } else {
                        System.out.println("Failed to create " + collectionFile.getName());
                        continue;
                    }
                } else {
                    System.out.println(collectionFile.getName() + " already existed.");
                }

                MongoCollection<Document> collection = database.getCollection(collectionName);
                JsonArray array = new JsonArray();

                // Gather all documents and create a JsonElement from them
                // and add them to the array
                for (Document document : collection.find()) {
                    // If you use the ObjectId from _id to find the document
                    // please don't use this... we won't correctly serialize it
                    // (I'll try to rewrite this to support it, shouldn't
                    // be hard, but i don't need this. So im not focused on it)
                    if (document.get("_id") instanceof ObjectId) {
                        document.remove("_id");
                    }
                    array.add(gson.toJsonTree(document));
                }
                FileUtil.write(gson.toJson(array), collectionFile);
            } catch (Exception ex) {
                System.out.println("Failed to save " + collectionName + ".");
                System.out.println(ex.getMessage());
            }
        }
        System.out.println("Completed export in " + (System.currentTimeMillis() - start) + "ms");
    }

}
