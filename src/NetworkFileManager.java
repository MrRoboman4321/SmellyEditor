/* -*-mode:java; c-basic-offset:2; indent-tabs-mode:nil -*- */
/**
 * This program will demonstrate the file transfer from local to remote.
 *   $ CLASSPATH=.:../build javac ScpTo.java
 *   $ CLASSPATH=.:../build java ScpTo file1 user@remotehost:file2
 * You will be asked passwd. 
 * If everything works fine, a local file 'file1' will copied to
 * 'file2' on 'remotehost'.
 *
 */
import com.jcraft.jsch.*;
import java.io.*;


//This is almost 100% blackbox code. Its black magic
public class NetworkFileManager {
    public NetworkFileManager()
    {

    }

    public String sendFile(String localFile, String remoteFile){
        /*if(arg.length!=2){
            System.err.println("usage: java ScpTo file1 user@remotehost:file2");
            System.exit(-1);
        }*/

        FileInputStream fis=null;
        try{

            String lfile = localFile;
            String user = "admin";
            String host = "10.19.83.2";
            String rfile = remoteFile;

            JSch jsch = new JSch();
            Session session=jsch.getSession(user, host, 22);

            // username and password will be given via UserInfo interface.
            //UserInfo ui=new MyUserInfo();

            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword("");
            session.connect();

            boolean ptimestamp = true;

            // exec 'scp -t rfile' remotely
            String command="scp " + (ptimestamp ? "-p" :"") +" -t "+rfile;
            Channel channel=session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream out=channel.getOutputStream();
            InputStream in=channel.getInputStream();

            channel.connect();

            if(checkAck(in)!=0){
                System.exit(0);
            }

            File _lfile = new File(lfile);

            if(ptimestamp){
                command="T"+(_lfile.lastModified()/1000)+" 0";
                // The access time should be sent here,
                // but it is not accessible with JavaAPI ;-<
                command+=(" "+(_lfile.lastModified()/1000)+" 0\n");
                out.write(command.getBytes()); out.flush();
                if(checkAck(in)!=0){
                    System.exit(0);
                }
            }

            // send "C0644 filesize filename", where filename should not include '/'
            long filesize=_lfile.length();
            command="C0644 "+filesize+" ";
            if(lfile.lastIndexOf('/')>0){
                command+=lfile.substring(lfile.lastIndexOf('/')+1);
            }
            else{
                command+=lfile;
            }
            command+="\n";
            out.write(command.getBytes()); out.flush();
            if(checkAck(in)!=0){
                System.exit(0);
            }

            // send a content of lfile
            fis=new FileInputStream(lfile);
            byte[] buf=new byte[1024];
            while(true){
                int len=fis.read(buf, 0, buf.length);
                if(len<=0) break;
                out.write(buf, 0, len); //out.flush();
            }
            fis.close();
            fis=null;
            // send '\0'
            buf[0]=0; out.write(buf, 0, 1); out.flush();
            if(checkAck(in)!=0){
                System.exit(0);
            }
            out.close();

            channel.disconnect();
            session.disconnect();

            return "Success!";
        }
        catch(Exception e){
            System.out.println(e);
            try{if(fis!=null)fis.close();}catch(Exception ee){}

            return "Failed, check stack trace";
        }
    }

    public String recvFile(String localFile, String remoteFile){

        FileOutputStream fos=null;
        try{

            String user = "admin";
            String host = "10.19.83.2";
            String rfile = remoteFile;
            String lfile = localFile;

            String prefix=null;
            if(new File(lfile).isDirectory()){
                prefix=lfile+File.separator;
            }

            JSch jsch=new JSch();
            Session session=jsch.getSession(user, host, 22);

            // username and password will be given via UserInfo interface.

            session.setConfig("StrictHostKeyChecking", "no");
            session.setPassword("");
            session.connect();

            // exec 'scp -f rfile' remotely
            String command="scp -f "+rfile;
            Channel channel=session.openChannel("exec");
            ((ChannelExec)channel).setCommand(command);

            // get I/O streams for remote scp
            OutputStream out=channel.getOutputStream();
            InputStream in=channel.getInputStream();

            channel.connect();

            byte[] buf=new byte[1024];

            // send '\0'
            buf[0]=0; out.write(buf, 0, 1); out.flush();

            while(true){
                int c=checkAck(in);
                if(c!='C'){
                    break;
                }

                // read '0644 '
                in.read(buf, 0, 5);

                long filesize=0L;
                while(true){
                    if(in.read(buf, 0, 1)<0){
                        // error
                        break;
                    }
                    if(buf[0]==' ')break;
                    filesize=filesize*10L+(long)(buf[0]-'0');
                }

                String file=null;
                for(int i=0;;i++){
                    in.read(buf, i, 1);
                    if(buf[i]==(byte)0x0a){
                        file=new String(buf, 0, i);
                        break;
                    }
                }

                //System.out.println("filesize="+filesize+", file="+file);

                // send '\0'
                buf[0]=0; out.write(buf, 0, 1); out.flush();

                // read a content of lfile
                fos=new FileOutputStream(prefix==null ? lfile : prefix+file);
                int foo;
                while(true){
                    if(buf.length<filesize) foo=buf.length;
                    else foo=(int)filesize;
                    foo=in.read(buf, 0, foo);
                    if(foo<0){
                        // error
                        break;
                    }
                    fos.write(buf, 0, foo);
                    filesize-=foo;
                    if(filesize==0L) break;
                }
                fos.close();
                fos=null;

                if(checkAck(in)!=0){
                    System.exit(0);
                }

                // send '\0'
                buf[0]=0; out.write(buf, 0, 1); out.flush();
            }

            session.disconnect();

            return "Success!";
        }
        catch(Exception e){
            System.out.println(e);
            try{if(fos!=null)fos.close();}catch(Exception ee){}

            return "Failed, check stack trace";
        }
    }

    static int checkAck(InputStream in) throws IOException{
        int b=in.read();
        // b may be 0 for success,
        //          1 for error,
        //          2 for fatal error,
        //          -1
        if(b==0) return b;
        if(b==-1) return b;

        if(b==1 || b==2){
            StringBuffer sb=new StringBuffer();
            int c;
            do {
                c=in.read();
                sb.append((char)c);
            }
            while(c!='\n');
            if(b==1){ // error
                System.out.print(sb.toString());
            }
            if(b==2){ // fatal error
                System.out.print(sb.toString());
            }
        }
        return b;
    }
}