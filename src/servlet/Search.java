package servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import net.sf.json.JSONObject;

public class Search extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static String INDEX_DIR = "";
	private static String DATA_DIR = "";
	private static Directory directory = null;
	private static Analyzer analyzer = null;
	
	public Search() {
        super();
    }
    
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		
		String field=request.getParameter("field");//��ȡ������Χ
		String keyword=(request.getParameter("kw"));//��ȡ�������
		String freshflag=(request.getParameter("freshflag"));//��ȡ�������
		DATA_DIR=request.getParameter("DATA_DIR");//��ȡ�ļ�·��
		INDEX_DIR=request.getParameter("INDEX_DIR");//��ȡ����·��
		
		//��ȡ�ļ�����
		String txt=(request.getParameter("txt"));
		String doc=(request.getParameter("doc"));
		String xls=(request.getParameter("xls"));
		String pdf=(request.getParameter("pdf"));
		String ppt=(request.getParameter("ppt"));
		String html=(request.getParameter("html"));
		
		try{
			//���ˢ�±�ʶ������Ϊ�գ���������������ֱ�������ؼ���
			if(freshflag!=null){
				String docType ="<!doctype html public \"-//w3c//dtd html 4.0 " +
						"transitional//en\">\n";
				IndexManager.setpath(DATA_DIR, INDEX_DIR);//��ʼ���ļ�·��������·��
				File fileIndex = new File(INDEX_DIR);
	            IndexManager.deleteDir(fileIndex);
	            fileIndex.mkdir();
	           IndexManager.createIndex(DATA_DIR);//��DATA_DIR�µ��ļ���INDEX_DIR�½�������
	           
	           out.println(docType +
	          		    "<html>\n" +
	          		    "<head><title>" + "�������"+ "</title></head>\n" +
	          		    "<body bgcolor=\"#f0f0f0\">\n" +
	          		    "<h1 align=\"center\">" + "�����ɹ�"+"\n"+"</h1>\n" +
	          		    "<ul>\n" +
	          		    "</ul>\n" +
	          		    "  <li><b>�ļ�·��</b>��"
	          		    + DATA_DIR +"<br/>"+"<br/>"+"  <li><b>����·��</b>��"+INDEX_DIR+"<br/>"
	          		    +"</body></html>");
			}
			//������������
			else{ 
				directory = FSDirectory.open(new File(INDEX_DIR));//���������·��
	            analyzer = new StandardAnalyzer(Version.LUCENE_40);//��ʼ���ʷ�������
	            DirectoryReader ireader = DirectoryReader.open(directory);
	            IndexSearcher isearcher = new IndexSearcher(ireader);//����������
	    
	           //�ж�������Χ�����ݲ�ͬ��������Χ���ɲ�ͬ��query1
	            Query query1;
	           if(field.equals("all")){
	        	   String[] fields={"filename","content"};
	        	   MultiFieldQueryParser mparser=new MultiFieldQueryParser(Version.LUCENE_40, fields, analyzer);//���������﷨������
		           query1 =mparser.parse(keyword);
	           }
	           else if(field.equals("filename")){
	        	   QueryParser parser1 = new QueryParser(Version.LUCENE_40, "filename", analyzer);
		           query1 = parser1.parse(keyword);

	           }
	           else{
	        	   QueryParser parser1 = new QueryParser(Version.LUCENE_40, "content", analyzer);	        
	        	   query1 = parser1.parse(keyword);
	           } 
	           //������ȡ���ļ���������query2
	            QueryParser parser2 = new QueryParser(Version.LUCENE_40, "type", analyzer);
	            Query query2 = parser2.parse(txt+" "+doc+" "+xls+" "+pdf+" "+ppt+" "+html);
	           //�ϲ� query1��query2
	            BooleanQuery q=new BooleanQuery();
	            q.add(query1,Occur.MUST);
	            q.add(query2,Occur.MUST);
	            //���ø�����ʾ
	            QueryScorer score= new QueryScorer(query1);
	            Fragmenter fragmenter=new SimpleSpanFragmenter(score);
	            SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("", "");
	            Highlighter highlighter=new Highlighter(simpleHTMLFormatter,score);
	            highlighter.setTextFragmenter(fragmenter);
	            
	            ScoreDoc[] hits = isearcher.search(q, null, 1000).scoreDocs;//�����������
	            JSONObject kword = new JSONObject();
	            JSONObject number = new JSONObject();
	    		kword.put("keyword", keyword);
	    		number.put("number", hits.length);
	    		//���عؼ��������н����
	        	out.write(kword.toString());
	        	out.write(number.toString());
	           
	        	for (int i = 0; i < hits.length; i++) {
	                Document hitDoc = isearcher.doc(hits[i].doc);
	                //��ȡ�ļ������˵���׺
	                String filename=hitDoc.get("filename");
	                String text1=filename.substring(0,filename.lastIndexOf("."));
	               //��ȡ�ļ�����
	                String text2=hitDoc.get("content");
	               //���ļ������и���Ԥ����
	                TokenStream tokenstream=analyzer.tokenStream("filename", new StringReader(text1));
		            String str1 = highlighter.getBestFragment(tokenstream, text1);
		            //���ļ����ݽ��и���Ԥ����
		            tokenstream=analyzer.tokenStream("content", new StringReader(text2));
		            String str2 = highlighter.getBestFragment(tokenstream, text2);
	              
		            if(str1==null)
	            	   str1=text1;
	                if(str2==null)
            	       str2="";
	                String path=hitDoc.get("path");
	                String type=hitDoc.get("type");
		            String newpath=path.substring(0,path.lastIndexOf("\\")+1)+str1+"."+type;
		        	JSONObject json = new JSONObject();
		    		json.put("path", newpath);
		    		json.put("content", str2);
		            //����������� 
		    		out.write(json.toString().replace("\\r","").replace("\\n",""));
	            }
	            ireader.close();
	            directory.close();
	            out.flush();
	            out.close();
			}
	        }catch(Exception e){
	            //e.printStackTrace();
	        	System.out.println(e.getMessage());
	            out.println(
            			"<html>\n" +
            			"<body bgcolor=\"#f0f0f0\">\n" +
            			"<h1 align=\"center\">" + "�����쳣"+"\n"+"</h1>\n"+ 
            			"</body></html>");
	        }
	
	}
}