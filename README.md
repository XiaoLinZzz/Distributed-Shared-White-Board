# Distributed Shared Whiteboard

## Introduction

This repo is an implementation of the assignment 2 for the course COMP90015: Distributed Systems. The details can find in the 
[Assignment 2](Project2-Sem1-2023.pdf).

---

This projects consists of a simple real-time white board which allows multiple users to draw on the same canvas. The whiteboard is implemented using Java and SwingUI.
Remote Method Invocation (RMI) is adopted to keep the real-time, efficient, and reliable communication between multiple JVMs.


## GUI Design

<img src="Picture 1.png">

## Language and Tools
<div style="display: flex; flex-direction: row; gap: 10px;">
    <img src="https://img.shields.io/static/v1?label=Java&message=SE11&color=F7DF1E&style=for-the-badge&logo=Oracle" alt="Java Badge">
    <img src="https://img.shields.io/static/v1?label=VSCode&message=1.60.0&color=007ACC&style=for-the-badge&logo=visual-studio-code" alt="VSCode Badge">
    <img src="https://img.shields.io/static/v1?label=Git&message=2.40.0&color=F05032&style=for-the-badge&logo=git" alt="VSCode Badge">
    <img src="https://img.shields.io/static/v1?label=Apache Maven&message=3.9.1&color=C71A36&style=for-the-badge&logo=Apache-Maven" alt="VSCode Badge">
    <img src="https://img.shields.io/static/v1?label=Gradle&message=8.0.2&color=02303A&style=for-the-badge&logo=Gradle" alt="VSCode Badge">
    <img src="https://img.shields.io/static/v1?label=intellij idea&message=2023.1.2&color=000000&style=for-the-badge&logo=Intellij IDEA" alt="IDEA Badge">
</div>


[//]: # (## Folder Instructions)

[//]: # ()
[//]: # (<img src="Instructure.PNG">)


## How to compile and Run

### Compile
1. Compile the server and client files using the following command:
```shell
cd src

javac WhiteboardClient.java
javac WhiteboardServer.java
```

2. Run the server with the following command:

   (the command should follow the format: java WhiteboardServer <host> <port> <username>)
```shell
java WhiteboardServer localhost 1099 Tom
```

3. Run the client with the following command:

   (the command should follow the format: java WhiteboardClient <host> <port> <username>)

    Note: The client should get the same `host` and `port` with the server.
```shell
java WhiteboardClient localhost 1099 Jerry
```

### Or

We also provide `Jar` file, you can run the server and client with the following command:

Note: The client should get the same `host` and `port` with the server.

For the server:
```shell [server]
java -jar CreateWhiteBoard.jar localhost 1099 Tom
```

For the client:
```shell [client]
java -jar JoinWhiteBoard.jar localhost 1099 Tom
```


## Interaction Diagram

<img src="Picture 2.png">


## Report
You can check the report in the [Report.pdf](DS_A2_report.pdf)


## License
This project is open-source and free to use under the [MIT License](LICENSE).