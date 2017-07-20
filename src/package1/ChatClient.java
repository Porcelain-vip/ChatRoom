package package1;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;

public class ChatClient {

    private TextArea TextArea = null; //显示框对象
    private JTextField jTextField = null; //输入框对象
    private Socket socket = null;
    private DataOutputStream dataOutputStream = null;
    private DataInputStream dataInputStream = null;
    private boolean connected = false;

    public static void main(String[] args) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                //建立一个客户端对象
                ChatClient chatClient = new ChatClient();
                chatClient.launch();
            }
        });
    }

    private void launch() {
        //初始化图形界面
        this.initJFrame();
        //启动客户端线程
        this.getChatClientThread().start();
    }

    private void initJFrame() {

        JFrame jFrame = new JFrame("客户端");
        jFrame.setLocation(300, 300);
        jFrame.setSize(400, 200);

        TextArea = new TextArea();
        TextArea.setEditable(false);
        jTextField = new JTextField();

        jFrame.add(TextArea, BorderLayout.NORTH);
        jFrame.add(jTextField, BorderLayout.SOUTH);

        jFrame.pack();
        jFrame.setVisible(true);

        jFrame.addWindowListener(new WindowAdapter() { //响应关闭窗口事件
            public void windowClosing(WindowEvent e) {
                //断开与服务器的连接
                setDisconnected();
                System.exit(0);
            }
        });
    }

    private Thread getChatClientThread() {
        return new Thread(new ChatClientThread());
    }

    //打开客户端连接
    private void setConnected(boolean connected) {
        try {
            //建立于服务器的连接，指明连接服务器的ip地址和端口
            socket = new Socket("127.0.0.1", 30000); //建立客户端对象

            OutputStream outputStream = socket.getOutputStream();
            InputStream inputStream = socket.getInputStream();

            dataOutputStream = new DataOutputStream(outputStream);
            dataInputStream = new DataInputStream(inputStream);

            this.connected = connected;
        } catch (UnknownHostException ue) {
            System.out.println("UnknownHostException");
            //ue.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    //断开客户端连接
    private void setDisconnected() {
        try {
            //关闭套接字也会间接关闭输入输出流
            socket.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    //运行在本地计算机上的客户端线程
    private class ChatClientThread implements Runnable {
        @Override
        public void run() {
            //建立和服务器端的连接
            setConnected(true);
            //也可以使用lambda表达式代替
            jTextField.addActionListener(new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {

                    String str = jTextField.getText();
                    jTextField.setText("");
                    try {
                        //与服务端建立输出流
                        dataOutputStream.writeUTF(str);
                    } catch (IOException ioe) {
                        ioe.printStackTrace();
                    }
                }
            }); //响应输入事件

            //不停地接收服务器端发送过来的数据，并显示
            while (connected) {
                String str = null;
                try {
                    //接收服务端的数据
                    str = dataInputStream.readUTF();
                } catch (EOFException ignored) {
                } catch (SocketException se) {
                    System.out.println("Socket closed");
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }
                TextArea.setText(TextArea.getText() + str + System.lineSeparator());//显示到显示框中
            }
        }
    }
}
