# Car Image/Text Recognition

## Assignment Information
An image recognition pipeline in AWS, using two parallel EC2 instances, S3, SQS, and Rekognition.

**Goal**: The purpose of this individual assignment is to learn how to use the Amazon AWS cloud platform and how to develop an AWS application that uses existing cloud services. Specifically, you will learn:
1. How to create VMs (EC2 instances) in the cloud.
2. How to use cloud storage (S3) in your applications.
3. How to communicate between VMs using a queue service (SQS).
4. How to program distributed applications in Java on Linux VMs in the cloud, and
5. How to use a machine learning service (AWS Rekognition) in the cloud.

**Description**: You have to build an image recognition pipeline in AWS, using two EC2 instances, S3, SQS, and Rekognition. The assignment must be done in Java on Amazon Linux VMs. For the rest of the description, you should refer to the figure below:

![AWS Archtecture](https://github.com/abe-min/CS-643-Programming-Assignment-1/blob/main/files/AWS_Arch.jpg?raw=true "AWS Archtecture")

Your have to create 2 EC2 instances (EC2 A and B in the figure), with Amazon Linux AMI, that will work in parallel. Each instance will run a Java application. Instance A will read 10 images from an S3 bucket that we created (https://njit-cs-643.s3.us-east-1.amazonaws.com) and perform object detection in the images. When a car is detected using Rekognition, with confidence higher than 90%, the index of that image (e.g., 2.jpg) is stored in SQS. Instance B reads indexes of images from SQS as soon as these indexes become available in the queue, and performs text recognition on these images (i.e., downloads them from S3 one by one and uses Rekognition for text recognition). Note that the two instances work in parallel: for example, instance A is processing image 3, while instance B is processing image 1 that was recognized as a car by instance A. When instance A terminates its image processing, it adds index -1 to the queue to signal to instance B that no more indexes will come. When instance B finishes, it prints to a file, in its associated EBS, the indexes of the images that have both cars and text, and also prints the actual text in each image next to its index.

# Assignment Implementation
1. Login to the Student AWS Learner account through the link sent by the TA.
![Learner Lab Home](https://github.com/abe-min/CS-643-Programming-Assignment-1/blob/main/files/Learner_Lab_Home.PNG?raw=true "AWS Student Learner Lab Home")

2. Within the Student AWS Learner account, navigate to Courses>ALLv1-38241>Modules>**Learner Lab**.
3. Click Start Lab and follow the **AWS** link to get to the AWS management console
4. Within AWS Management console naviage to EC2. Once in the EC2 section, Click on  **Launch Instances**

## Creating EC2 Instances; Do the following steps twice to create 2 EC2 instances:
1. Enter the name of the EC2 instance; EC2 A will run the Image recognition part of the code; EC2 B will run the Text recognition part of the code 
2. Select AMI **Amazon Linux 2023 AMI (HVM)**.
3. Select **t2.micro** for T2 instances
4. Create a new key pair, in my case named **mar15**
5. Under Network Settings, select Create security group and check the below settings.
	-  [x] Allow SSH traffic from
	-  [x] Allow HTTPs traffic from the internet
	-  [x] Allow HTTP traffic from the internet
	- And instead of **Anywhere**, select **My IP** to only send traffic from PC's IP address.

After completion, two Instances My instances are named **EC2 A** and **EC2 B** and looks like this:

![Running EC2 Instances](https://github.com/abe-min/CS-643-Programming-Assignment-1/blob/main/files/Active_Instances.PNG?raw=true "2 Running EC2 Instances")

### Add IAM roles to the created instances:
1. Select an IAM role with the following permissions as policies:
	- [AmazonSQSFullAccess](https://us-east-1.console.aws.amazon.com/iam/home#/policies/arn:aws:iam::aws:policy/AmazonSQSFullAccess)
	- [AmazonS3FullAccess](https://us-east-1.console.aws.amazon.com/iam/home#/policies/arn:aws:iam::aws:policy/AmazonS3FullAccess)
	- [AmazonRekognitionFullAccess](https://us-east-1.console.aws.amazon.com/iam/home#/policies/arn:aws:iam::aws:policy/AmazonRekognitionFullAccess)

These policies are necessary to use AWS' S3, SQS, and Rekognition services


## JAVA programs:
1. There will be two JAVA programs. The first will be for **Object** recognition and the second is for  **Text** recognition.
2. Per the project instructions, the object detection program will run on the first instance (EC2 A) while the second instance (EC2 B) will run the text detection program.	
4. The programs will be packaged using Apache Maven and exported to a .jar file. This make sit easy to transfer the progam to the EC2 instances as well as allow for easy execution in the EC2 VM.
5. To transfer the files, I will be using WinSCP to transfer the prgrams from my Windows PC. 

### SSH Access from PC command prompt:
2.  Open a command prompt window, and change directory `cd` to the directory where the **.pem** file was downloaded in the above EC2 creation step. In my case my .pem file is called **mar15**

3. Change the security on the file to be visible for my user on the PC only. 
  
4. In the AWS Management Console, and in the EC2 service, navigate to the **IPv4 Public IP** of the instance to conenct to. This is the IP addr for the SSH session. 
    
6. In the windows command prompy, run the command: 

   `ssh -i "mar15.pem" ec2-user@<**IPv4 Public IP**>.amazonaws.com` 
    
7.  Type `yes` when prompted to allow a first connection to this remote SSH server.

Once an SSH connection is made to the EC2 instance, it looks like this:

![SSH connection](https://github.com/abe-min/CS-643-Programming-Assignment-1/blob/main/files/SSH_EC2.PNG?raw=true "Active SSH Connection")



### Run the 2 programs in the EC2 instance:
1. I am using WinSCP to tranfer the .jar files to the EC2 instance. To do this I use the mar15.pem file; convert it toa .pkk file (putty security file). Using this file I can establish an SFTP connection between my PC and the EC2 instance. 
2. Runt he JAVA progam via the command `java -jar aws-object-rekognition-1.0-SNAPSHOT.jar`
3. For the second program since we want to output the result in a text file, we can use the command `java -jar aws-text-rekognition-0.0.1-SNAPSHOT.jar > EC2B_output.txt`. 

#### Running Object Detection program on EC2-A instance:
1. After executing the command `java -jar aws-object-rekognition-1.0-SNAPSHOT.jar` imaghes that satisfy the condition of containing a car with a confidence rate of >90 will be pushed to the SQS (ImageRecognitionQue).
![Images Pushed to Que](https://github.com/abe-min/CS-643-Programming-Assignment-1/blob/main/files/SQS_Images_Entry.PNG?raw=true "SQS pushed")

2. There are 6 items that are pushed to the queue, having satisfied the above condition
![New Que](https://github.com/abe-min/CS-643-Programming-Assignment-1/blob/main/files/Ques.PNG?raw=true "Created Que")
3. The contents of the ImageRecognitionQue queue:
![List of Que Content](https://github.com/abe-min/CS-643-Programming-Assignment-1/blob/main/files/SQS_messages_poll.PNG?raw=true "List of Que Content")


#### Running program on EC2-B instance:
1. The second progam will read from the Que the 6 images were insterted to during the first object recognition progam in EC2 A. Do the same steps from the previosu steps to connect to EC2 B and run the folowing command to excute the second JAVA program (Text recognition): `java -jar aws-text-rekognition-0.0.1-SNAPSHOT.jar > EC2B_output.txt`
![EC2 B](https://github.com/abe-min/CS-643-Programming-Assignment-1/blob/main/files/ec2-B-TEXT_Detection.PNG?raw=true "EC2 B Running")

