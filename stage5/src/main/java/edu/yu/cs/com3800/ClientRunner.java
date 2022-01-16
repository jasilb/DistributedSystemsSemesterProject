package edu.yu.cs.com3800;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.logging.Logger;


public class ClientRunner {
    public static void main(String[] args) throws MalformedURLException, InterruptedException {
         String validClass = "package edu.yu.cs.fall2019.com3800.stage1;\n\npublic class HelloWorld\n{\n    public String run()\n    {\n        return \"Hello world!\";\n    }\n}\n";

        ClientImpl client = null;

        if (args.length==0){
            client = new ClientImpl("localhost",8888,0);
        }
        else {
            client = new ClientImpl("localhost",8888,Integer.parseInt(args[0]));
        }




        if (args.length==0){
            client.changeURL("gossip");
            client.sendCompileAndRunRequest(validClass);
        }
        else {
            String code = validClass.replace("world!", "world! from code version " + args[0]);
            client.sendCompileAndRunRequest(code);
        }

        client.run();

    }
}


