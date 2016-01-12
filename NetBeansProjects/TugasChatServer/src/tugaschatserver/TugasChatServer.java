/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package tugaschatserver;
import java.io.*;
import java.net.*;
import java.util.*;
/**
 *
 * @author Hermawan
 */
public class TugasChatServer {
    private static int uniqueId;
    private ArrayList<TugasChatServer.ClientThread> clients;
    private int port;
    private boolean keepGoing;
    public TugasChatServer() {
        //Inisialisasi server port
        this.port = 9000;
        clients = new ArrayList<>();
    }
 
    public void start() {
        keepGoing = true;
        try {
            //pembuatan server socket untuk melisten berdasarkan variabel port (9000)
            ServerSocket listenSocket = new ServerSocket(port);
            while (keepGoing) {
                System.out.println("ChatServer waiting for Clients on port " + port + ".");
                //pembuatan socket untuk komunikasi dengan satu client
                Socket socket = listenSocket.accept();
                if (!keepGoing) {
                    break;
                }
                //pembuatan client thread dengan nama "t"
                TugasChatServer.ClientThread t = new TugasChatServer.ClientThread(socket);
                //penambahan anggota client ke array "clients"
                clients.add(t);
                //pembukaan thread untuk satu client
                t.start();
                //penulisan status saja~
                send("login~" + t.username + "~" + t.username + " sedang login...~Server~\n");
                
            }
            try {
                //penutupan seluruh thread
                listenSocket.close();
                for (int i = 0; i < clients.size(); ++i) {
                    TugasChatServer.ClientThread tc = clients.get(i);
                    try {
                        tc.sInput.close();
                        tc.sOutput.close();
                        tc.socket.close();
                    } catch (IOException ioE) {
                    }
                }
            } catch (Exception e) {
                System.out.println("Exception closing the server and clients: " + e);
            }
        } catch (IOException e) {
            String msg = "Exception on new ServerSocket: " + e + "\n";
            System.out.println(msg);
        }
    }
    
    public boolean pribadi;
    public String validasiNama;
    private synchronized void send(String message) {
        /*if (message.split("~")[0] == "postPrivateText")
        {
            String validasiNama;
            validasiNama = message.split("~")[3];
            for(int i = clients.size(); --i >=0;){
                TugasChatServer.ClientThread pct = clients.get(i);
                if(validasiNama == pct.username)
                {
                    pct.writeMsg(message);
                    break;
                }
            }
        }*/
        
        validasiNama = message.split("~")[3];
        pribadi = false;
        if (message.split("~")[0].equals("recievePrivateText"))
        {
            pribadi = true;
            System.out.println("Terdeteksi pesan pribadi");
        }
        else{
            System.out.println("Bukan pesan pribadi");
        }
        for (int i = clients.size(); --i >= 0;) {
            //pengecekan apakah suatu thread dari satu client masih aktif atau tidak, jika tidak maka database client tersebut dihapus
            TugasChatServer.ClientThread ct = clients.get(i);
        
            if(pribadi){
                if(validasiNama.equals(ct.username)){
                    ct.writeMsg(message);
                    System.out.println("berhasil sama");
                   // break;
                }else{
                    System.out.println("tidak sama");
                    //break;
                }
            }
            else{
                //fungsi filtering untuk private message dan forwarsd ke sesuai usernamenya
                if (!ct.writeMsg(message)) {
                    //penghapusan client ke i
                    clients.remove(i);
                    System.out.println("Disconnected Client " + ct.username + " removed from list.");
                }
            }
        }
    }
 
    private String getClients() {
        //penulisan seluruh data username
        String s = "";
        for (ClientThread clientThread : clients) {
            s += clientThread.username + ":";
        }
        s += "---";
        System.out.println(s);
        return s;
    }
 
    private synchronized void remove(int id) {
        //penghapusan client dengan parameter id
        for (int i = 0; i < clients.size(); ++i) {
            TugasChatServer.ClientThread ct = clients.get(i);
            if (ct.id == id) {
                clients.remove(i);
                String iresponse = "list~server~" + getClients() + "~ ~ ~ ~ ~\n";
                send(iresponse);
                return;
            }
        }
    }
 
    public static void main(String[] args) {
        //pembuatan objek server dari class ini
        TugasChatServer server = new TugasChatServer();
        server.start();
    }
 
    private class ClientThread extends Thread {
 
        private Socket socket;
        private ObjectInputStream sInput;
        private ObjectOutputStream sOutput;
        private int id;
        private String username;
 
        public ClientThread(Socket socket) {
            id = ++uniqueId;
            this.socket = socket;
            //pembuatan input/output stream
            System.out.println("Menciptakan Object Input/Output Streams");
            try {
                sOutput = new ObjectOutputStream(socket.getOutputStream());
                sInput = new ObjectInputStream(socket.getInputStream());
                //perubahan data ke string dari object input stream
                String message = (String) sInput.readObject();
                username = message.split("~")[1];
                System.out.println(username + " masuk.");
            } catch (IOException e) {
                System.out.println("Exception creating new Input/output Streams: " + e);
            } catch (ClassNotFoundException e) {
            }
        }
 
        @Override
        public void run() {
            boolean keepGoing = true;
            while (keepGoing) {
 
                String message;
                try {
                    //pembacaan pesan dari objectinputsream sInput direkontruksi ke bentuk string dan dimasukkan ke variabel message
                    message = sInput.readObject().toString();
                    
                } catch (IOException e) {
                    System.out.println(username + " Exception reading Streams: " + e);
                    break;
                } catch (ClassNotFoundException e2) {
                    break;
                }
 
                String type = message.split("~")[0];
                String pengirim = message.split("~")[1];
                String text = message.split("~")[2];
                String kepada = message.split("~")[3];
                String response;
                
 
                switch (type) {
                    case "postText":
                        response = "recieveText~" + pengirim + "~" + text + "~" + kepada + "~\n";
                        send(response);
                        break;
                    case "postPrivateText":
                        response = "recievePrivateText~" + pengirim + "~" + text + "~" + kepada + "~\n";
                        send(response);
                        break;
                    case "login":
                        response = "login~" + pengirim + "~" + text + "~" + kepada + "~\n";
                        send(response);
                        break;
                    case "logout":
                        response = "logout~" + pengirim + "~" + text + "~" + kepada + "~\n";
                        send(response);
                        break;
                    case "list":
                        response = "list~server~" + getClients() + "~ ~ ~ ~ ~\n";
                        send(response);
                        break;
                }
            }
 
            remove(id);
            close();
        }
 
        private void close() {
            try {
                if (sOutput != null) {
                    sOutput.close();
                }
            } catch (Exception e) {
            }
            try {
                if (sInput != null) {
                    sInput.close();
                }
            } catch (Exception e) {
            }
            try {
                if (socket != null) {
                    socket.close();
                }
            } catch (Exception e) {
            }
        }
 
        private boolean writeMsg(String msg) {
            if (!socket.isConnected()) {
                close();
                return false;
            }
            try {
                //penulisan ke object output stream
                sOutput.writeObject(msg);
            } catch (IOException e) {
                System.out.println("Error sending message to " + username);
                System.out.println(e.toString());
            }
            return true;
        }
        
        
        
    }
}
