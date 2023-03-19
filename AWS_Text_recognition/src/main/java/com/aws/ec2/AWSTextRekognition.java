package com.aws.ec2;

//Spring Framework 
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

//AWS services/libraries 
import com.amazon.sqs.javamessaging.*;
import com.amazonaws.AmazonServiceException;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.*;
import com.amazonaws.services.s3.model.ListObjectsV2Request;
import com.amazonaws.services.s3.model.ListObjectsV2Result;
import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sqs.*;
import com.amazonaws.services.sqs.model.CreateQueueRequest;

// Import java utilities 
import java.util.*;
import com.amazonaws.services.rekognition.model.*;
import javax.jms.*;
import javax.jms.Queue;

import com.amazonaws.services.rekognition.AmazonRekognition;
import com.amazonaws.services.rekognition.AmazonRekognitionClientBuilder;

@SpringBootApplication

class MyListener implements MessageListener {

    @Override
    public void onMessage(Message message) {

        try {
            Regions clientRegion = Regions.US_EAST_1;
            String bucketName = "njit-cs-643";

            ListObjectsV2Request req = new ListObjectsV2Request().withBucketName(bucketName);
            ListObjectsV2Result result;

            AmazonS3 s3Client = AmazonS3ClientBuilder.standard()
                    .withRegion(clientRegion)
                    .build();

            AmazonRekognition rekognitionClient = AmazonRekognitionClientBuilder.standard()
                    .withRegion(clientRegion)
                    .build();

            result = s3Client.listObjectsV2(req);
            for (S3ObjectSummary objectSummary : result.getObjectSummaries()) {
                String m = (String) ((TextMessage) message).getText().toString();
                if (objectSummary.getKey().contains(m)) {

                    String photo = objectSummary.getKey();

                    DetectTextRequest request = new DetectTextRequest()
                            .withImage(new Image()
                                    .withS3Object(new S3Object()
                                            .withName(photo)
                                            .withBucket(bucketName)));
                    try {
                        DetectTextResult result1 = rekognitionClient.detectText(request);
                        List<TextDetection> textDetections = result1.getTextDetections();
                        if (!textDetections.isEmpty()) {
                            System.out.print("Text Detected lines and words for:  " + photo + " ==> ");
                            for (TextDetection text : textDetections) {

                                System.out.print("  Text Detected: " + text.getDetectedText() + " , Confidence: "
                                        + text.getConfidence().toString());
                                System.out.println();
                            }
                        }
                    } catch (AmazonRekognitionException e) {
                        System.out.print("Error");
                        e.printStackTrace();
                    }
                }
            }

        } catch (JMSException e) {
            System.out.println("First Instances has not run yet...");
        }
    }
}

public class AWSTextRekognition {
    public static void main(String[] args) throws Exception {
        SpringApplication.run(AWSTextRekognition.class, args);

        Regions clientRegion = Regions.US_EAST_1;

        try {
            AmazonSQSClientBuilder.standard()
                    .withRegion(clientRegion)
                    .build();

            try {

                SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(),
                        AmazonSQSClientBuilder.defaultClient());

                SQSConnection connection = connectionFactory.createConnection();

                AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();

                if (!client.queueExists("ImageRecognitionQue.fifo")) {
                    Map<String, String> attributes = new HashMap<String, String>();
                    attributes.put("FifoQueue", "true");
                    attributes.put("ContentBasedDeduplication", "true");
                    client.createQueue(
                            new CreateQueueRequest().withQueueName("ImageRecognitionQue.fifo").withAttributes(attributes));
                }

                Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                Queue queue = session.createQueue("ImageRecognitionQue.fifo");


                MessageConsumer consumer = session.createConsumer(queue);

                consumer.setMessageListener(new MyListener());


                connection.start();

                Thread.sleep(10000);

            } catch (Exception e) {
                System.out.println("Please run the Instance-1, the program will wait for the queue to have elements.");
                SQSConnectionFactory connectionFactory = new SQSConnectionFactory(new ProviderConfiguration(),
                        AmazonSQSClientBuilder.defaultClient());

                SQSConnection connection = connectionFactory.createConnection();

                AmazonSQSMessagingClientWrapper client = connection.getWrappedAmazonSQSClient();

                if (!client.queueExists("ImageRecognitionQue.fifo")) {
                    Map<String, String> attributes = new HashMap<String, String>();
                    attributes.put("FifoQueue", "true");
                    attributes.put("ContentBasedDeduplication", "true");
                    client.createQueue(
                            new CreateQueueRequest().withQueueName("ImageRecognitionQue.fifo").withAttributes(attributes));
                    Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);

                    Queue queue = session.createQueue("ImageRecognitionQue.fifo");

                    MessageConsumer consumer = session.createConsumer(queue);

                    consumer.setMessageListener(new MyListener());

                    connection.start();

                    Thread.sleep(10000);
                }
            }

        } catch (AmazonServiceException e) {
            System.out.println("Please run the Instance-1 first. Waiting...");
        }
    }
}
