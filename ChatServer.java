import java.net.*;
import java.io.*;
import java.util.*;

public class ChatServer {

	public static void main(String[] args) {
		try{
			//서버 준비
			//포트번호는 클라이언트와 일치
			//채팅 서버에서는 먼저 ServerSocket을 생성해서 해당 포트로 Connection 신호가 오기를 기다린다. (.accept())
			//서버 소켓 인스턴스 생성. 소켓넘버를 파라미터로 받고 클라이언트의 접속을 확인해준다.
			ServerSocket server = new ServerSocket(10001);//서버소켓 선언
			//소켓 획득
			System.out.println("Waiting connection...");//서버가 대기중
			//해시 맵 생성
			HashMap hm = new HashMap();
			while(true){
				//.accept() 메소드에서 반환하는 클라이언트 소켓을 통해 바로 Read, Write 하지않고 멀티 유저를 수용하기 위해 유저마다 쓰레드를 생성해준다.
				//클라이언트의 접속을 확인하고 동시에 소켓인스턴스를 생성한다.
				Socket sock = server.accept();//서버는 여기서 클라이언트를 기다린다. 서버 소켓 대기
				//이때부터 클라이언트가 서버의 10000port로 접속하면 Socket의 모든 정보가 구해지므로 accept()메소드가
        //Socket객체를 만들어서 반환.
        //Socket객체가 만들어졌다 -> 통신의 대상이 명확.
				//서버 프로그램의 스레드인 ChatThread를 생성한다.
				ChatThread chatthread = new ChatThread(sock, hm);
				chatthread.start();//쓰레드 실행
			} // while
		}catch(Exception e){
			System.out.println(e);
		}
	} // main
}

class ChatThread extends Thread{
	private Socket sock;
	private String id;
	private BufferedReader br;
	private HashMap hm;
	private boolean initFlag = false;
	public ChatThread(Socket sock, HashMap hm){
		this.sock = sock;
		this.hm = hm;
		try{
			//클라이언트와 통신을 위한 stream 생성
			//각 쓰레드에서는 쓰레드가 생성될 때 소켓 통신을 할 PrintWriter와 BufferedReader를 만들고 접속한 유저의 아이디와 출력스트림(PrintWriter)을 HashMap에 key, value로 넣는다.
			//문자열을 출력해 줄수 있는 필터스트림 장착
			//클라이언트에게 데이터를 전달해주는 출력 스트림 생성
			PrintWriter pw = new PrintWriter(new OutputStreamWriter(sock.getOutputStream()));
			//문자열을 빠르게 읽어들일 수 있는 버퍼 필터 스트림 장착
			//클라이언트로부터 데이터를 받을 입력 스트림 생성
			br = new BufferedReader(new InputStreamReader(sock.getInputStream()));
			id = br.readLine();//맨 첫번째 배열이었던 id를 읽어와 저장한다.
			broadcast(id + " entered.");// 같이 채팅하는 유저들에게 알린다.
			System.out.println("[Server] User (" + id + ") entered.");
			synchronized(hm){
				hm.put(this.id, pw);
			}
			initFlag = true;
		}catch(Exception ex){
			System.out.println(ex);
		}
	} // construcor
	//run()에서 쓰레드가 실행되면서 메시지가 들어오면 다른 유저 모두에게 broadcast로 메시지를 보낸다.
	public void run(){
		try{
			String line = null;
      String str = null;
			//BufferedReader의 readLine() 메소드를 이용해
      //클라이언트가 보내는 문자열 한 줄을 읽어들임
			while((line = br.readLine()) != null){
				//사용자가 종료를 선언하는 문자열을 입력했을 때 while문을 빠져나온다.
				if(line.equals("/quit"))
					break;
				//해당 문자열이 첫번째 글자의 위치번호를 반환한다. 만약 "/to"가 첫번째에 있다면 "/"의 위치 번호 0을 반환한다.
				if(line.indexOf("/to ") == 0){
					//귓속말 메소드에 전달
					sendmsg(line);
				}
        if((str = banned(line))!=null){//금지된 언어
          smbanned(str);
        }
        else if(line.equals("/userlist")){//userlist를 출력해준다.
          send_userlist();
        }
        else
				  //위의 어느 조건도 만족하지 않는다면 채팅방에 문자를 출력한다.
					broadcast(id + " : " + line);
			}
		}catch(Exception ex){
			System.out.println(ex);
		}finally{
			synchronized(hm){
				hm.remove(id);
			}
			broadcast(id + " exited.");
			try{
				if(sock != null)
					sock.close();//소켓 종료
			}catch(Exception ex){}
		}
	} // run
	public void sendmsg(String msg){
		//""이 위치하고 있는 번호에 1을 더해 ""다음이 시작지점임을 저장한다. 여기서는 "/to"<이부분>
		int start = msg.indexOf(" ") +1;
		//start이후 그 다음 ""가 나오는 곳의 위치번호를 저장하여 끝을 알린다.
		int end = msg.indexOf(" ", start);
		if(end != -1){
			//처음 번호와 과 끝번호 사이에 저장되어있는 문자를 출력한다. 여기서는 유저의 id를 의미한다.
			String to = msg.substring(start, end);
			//끝 번호 다음부터 메세지가 입력되므로 그다음부터 끝까지의 문자열을 ms2에 저장한다.
			String msg2 = msg.substring(end+1);
			//to에 해당하는 데이터 즉 출력 스트림의 참조값을 오브젝트 인스턴스에 저장. 모든 클래스가 Object클래스를 상속하기 때문에 가능
			Object obj = hm.get(to);
			if(obj != null){
				//해당 참조값을 pw에 저장
				PrintWriter pw = (PrintWriter)obj;
				//해당 출력 스트림을 가지고 잇는 사람에게 귓속말 전달
				pw.println(id + " whisphered. : " + msg2);
				pw.flush();
			} // if
		}
	} // sendmsg
	public void broadcast(String msg){
		synchronized(hm){
			//해시 맵에 저장되어있는 모든 출력 스트림의 참조값을 collection에 저장한다.
			Collection collection = hm.values();
			//iterator을 생성하여 가각의 데이터를 참조하여 접근하도록 한다.
			/*
			결국 해시맵에 저장되어있는 모든 사람에게 메시지를 전달하는 생성하는것이다.
			해당 데이터, 즉 출력 스트림으로 파라미터로 전달받은 메세지를 전송한다.
			*/
			Iterator iter = collection.iterator();
			//데이터가 있다면, PrintWriter pw = (PrintWriter)iter.next(); 참조 변수를 pw에 저장한다.
			while(iter.hasNext()){
				PrintWriter pw = (PrintWriter)iter.next();//수서대로 iterator로 pw받는다.
        PrintWriter pw1 = (PrintWriter)hm.get(id);//자신
				if(pw==pw1) continue;
				pw.println(msg);
				pw.flush();
			}
		}
	} // broadcast
  public void send_userlist(){//userlist를 받아온다.
    int j = 1;
		PrintWriter pw = null;
		Object obj = null;
		Iterator<String> iter = null;
		synchronized(hm){
			iter = hm.keySet().iterator();//id를 iterator로 받는다.
			obj = hm.get(id);
		}
		if(obj != null){
				pw = (PrintWriter)obj;//자신에게
		}
		pw.println("<User list>");
		while(iter.hasNext()){
				String list = (String)iter.next();
				pw.println(j+": "+list);//자신에게 리스트를 보여준다.
				j++;
		}
		j--;
		pw.println("Total : "+j+".");//전체 명수를 보여준다.
		pw.flush();
  }
  //금지된 언어
  public String banned(String msg){
    int wc = 1;
    String[] word = {"또라이", "미친", "바보", "병신", "등신", "새끼"};//금지된언어들
    for(int i=0;i<word.length;i++){
      if(msg.contains(word[i])){//금지된 언어가 있는지 검사하여 꺼낸다.
        return word[i];
      }
    }return null;
  }
  public void smbanned(String saying){//banned으로부터 금지된언어를 받는다
    Object obj = hm.get(id);
    if(obj!=null){
      PrintWriter pw = (PrintWriter)obj;//메세지를 보낸 자신
      pw.println(saying + "is banned message");//금지된 메세지라고 알려준다.
      pw.flush();
    }
  }
}
