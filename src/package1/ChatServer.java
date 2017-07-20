package package1;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class ChatServer {
    private boolean accepted = false;
    private ServerSocket serverSocket = null;
    private Socket socket = null;
    //可能会有多个客户端线程请求连接，那么就使用一个集合来保存这些客户端线程
    private List<ClientRunnable> clients = new ArrayList<>();

    public static void main(String[] args) {
        new ChatServer().launch();
    }

    private void setAccepted(boolean accepted) {
        this.accepted = accepted;
    }

    private void setDisconnected() {
        try {
            if (serverSocket != null) {
                serverSocket.close();
            }
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void launch() {
        try {
            //建立服务器端口号，ip地址就是本机ip
            serverSocket = new ServerSocket(30000);
            //开启服务端侦听客户端连接线程
            new Thread(new ServerSpyRunnable()).start();

        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private void runClientRunnable(Socket socket) {
        ClientRunnable clientRunnable = new ClientRunnable(socket);
        clients.add(clientRunnable); //向集合中添加客户端线程
        new Thread(clientRunnable).start(); //启动客户端线程
    }

    //服务端侦听客户端连接线程
    private class ServerSpyRunnable implements Runnable {
        @Override
        public void run() {
            try {
                setAccepted(true);
                //服务器端使用循环不停地监听客户端线程的出现
                while (accepted) {
                    //当没有客户端连接时会一直等待连接，直到有客户端连接为止
                    //这是一个阻塞的方法，可能会阻塞main线程，最好的方法是单独开启一个线程去实现服务器监听客户端的连接
                    socket = serverSocket.accept();
                    System.out.println("有客户端连接到了本机的30000端口");
                    runClientRunnable(socket);
                }
            } catch (BindException be) {
                System.out.println("30000端口被占用了");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                setDisconnected();
            }
        }
    }

    //服务器中的客户端线程
    private class ClientRunnable implements Runnable {

        private Socket socket = null;
        private DataInputStream dataInputStream = null;
        private DataOutputStream dataOutputStream = null;
        private boolean started = false;

        //运行在服务器端的客户端线程
        ClientRunnable(Socket socket) {
            this.socket = socket;
        }

        //建立与客户端的连接
        private void setClientStarted(boolean started) {
            try {
                InputStream inputStream = socket.getInputStream();
                OutputStream outputStream = socket.getOutputStream();

                dataInputStream = new DataInputStream(inputStream);
                dataOutputStream = new DataOutputStream(outputStream);

                this.started = started;
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }
        }

        //断开与客户端的连接
        private void setClientEnded() {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        @Override
        public void run() {
            try {
                setClientStarted(true);
                //用一个循环不停地监视各个客户端发送过来得套接字，然后服务端再向各个客户端转发套接字
                while (started) {
                    //服务器端接收客户端发送过来的数据
                    String str = dataInputStream.readUTF();
                    //服务器端向所有客户端发送消息
                    for (ClientRunnable c : clients) {
                        c.dataOutputStream.writeUTF(str);
                    }
                }
            } catch (EOFException ee) {
                System.out.println("有客户端退出了");
            } catch (IOException ioe) {
                ioe.printStackTrace();
            } finally {
                setClientEnded();
            }
        }
    }
}
