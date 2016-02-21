package org.egale.core;

import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.UpdateOptions;
import java.util.ArrayList;
import java.util.List;
import org.bson.Document;

/**
 *
 * @author Zied
 */
public class MongoTest {

    static int index = 0;

    //Pojo used to populate data
    static class CheckModel {

        public String host;  //  client alias
        public String chkId; // check id
        public String chkName; // check name
        public String cmd;  // command 
        public String desc; // description
        public String topic; // jms topic
        public int rrate = 60; // refresh rate
        public int status;  // the status of the execution
        public String out; //result
    }

    static MongoClient mongoClient = new MongoClient();
    static String dbName = "eagle";

    private static List<Document> getCheckValues(CheckModel checkModel, int index) {

        final List<Document> checkValues = new ArrayList<>();
        final Document val = new Document()
                .append("id", index)
                .append("output", checkModel.out)
                .append("status", checkModel.status);
        checkValues.add(val); // second execution should not ovveride the content of value but a new 
        return checkValues;
    }

    private static Document buildClientDocument(MongoDatabase db, CheckModel checkModel) {

        int idx = ++index % 20;
        final List<Document> checks = new ArrayList<>();
        final Document check = new Document()
                .append("check_name", checkModel.chkName)
                .append("command", checkModel.cmd)
                .append("id", checkModel.chkId)
                .append("description", checkModel.desc)
                .append("topic", checkModel.topic)
                .append("last_output", checkModel.out)
                .append("index", index)
                .append("last_status", checkModel.status)
                .append("values", getCheckValues(checkModel, idx))
                .append("refresh", checkModel.rrate);
        checks.add(check);

        Document client = new Document()
                .append("name", checkModel.host)
                .append("checks", checks);
        //.append("$addToSet" , new Document("checks", checks)); // <<- error here '$addToSet' is not recocnized 
        return client;

    }

    // Name of the topic from which we will receive messages from = " testt"
    public static void main(String[] args) {
        MongoDatabase db = mongoClient.getDatabase(dbName);

        CheckModel checkModel = new CheckModel();
        checkModel.cmd = "ls -lA";
        checkModel.host = "client_001";
        checkModel.desc = "ls -l command";
        checkModel.chkId = "lsl_command";
        checkModel.chkName = "ls command";
        checkModel.out = "result of ls -l";
        checkModel.status = 0;
        checkModel.topic = "basic_checks";
        checkModel.rrate = 5000;

        initDB(db);
        // insert the first client
        db.getCollection("clients") // execute client insert or update
                .updateMany(
                        new Document().append("_id", checkModel.host), new Document("$set", buildClientDocument(db, checkModel)), new UpdateOptions().upsert(true)
                );
        //--------------------- SECOND INSERT ----- Disable this to get first result posted on stackovertflow
//         insert the second client after doing some data modifications
        db.getCollection("clients") // execute client insert or update
                .updateMany(//                                                                                                VVV
                        //                                                                                           ***modif check model***
                        new Document().append("_id", checkModel.host), new Document("$set", buildClientDocument(db, modifyData(checkModel))), new UpdateOptions().upsert(true)
                );
    }

    // mdofiy data to test the check
    private static CheckModel modifyData(CheckModel checkModel) {
        checkModel.status = 1;
        checkModel.out = "ls commadn not found";
        return checkModel;
    }

    private static void initDB(MongoDatabase db) {

        final MongoCollection<Document> collection = db.getCollection("configuration");
        if (collection.count() == 0) {
            Document b = new Document()
                    .append("_id", "app_config")
                    .append("historical_data", 20)
                    .append("current_index", 0);
            collection.insertOne(b);
        }

        final Document lambdaDoc = new Document().append("none", "none");
        final MongoCollection<Document> clients = db.getCollection("clients");
        clients.insertOne(lambdaDoc);
        clients.deleteOne(lambdaDoc);

        final MongoCollection<Document> topics = db.getCollection("topics");
        topics.insertOne(lambdaDoc);
        topics.deleteOne(lambdaDoc);
    }

}
