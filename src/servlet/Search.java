package servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringReader;
import java.util.List;

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



public class Search extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static String INDEX_DIR = "C:\\LCT\\Work\\data\\LuceneIndex";
	private static Directory directory = null;
	private static Analyzer analyzer = null;
   
	public Search() {
        super();
    }
   
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// 设置响应内容类型
		response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		
		String title ="搜索结果";
		String field=request.getParameter("field");
		String keyword=(request.getParameter("kw"));
		String txt=(request.getParameter("txt"));
		String doc=(request.getParameter("doc"));
		String xls=(request.getParameter("xls"));
		String pdf=(request.getParameter("pdf"));
		String ppt=(request.getParameter("ppt"));
		String docType ="<!doctype html public \"-//w3c//dtd html 4.0 " +
		"transitional//en\">\n";
		
		try{
	            directory = FSDirectory.open(new File(INDEX_DIR));
	            analyzer = new StandardAnalyzer(Version.LUCENE_40);
	            DirectoryReader ireader = DirectoryReader.open(directory);
	            IndexSearcher isearcher = new IndexSearcher(ireader);
	    
	           Query query1;
	           if(field.equals("all")){
	        	   String[] fields={"filename","content"};
	        	   MultiFieldQueryParser mparser=new MultiFieldQueryParser(Version.LUCENE_40, fields, analyzer);
		           query1 =mparser.parse(keyword);
	           }
	           else if(field.equals("filename")){
	        	   QueryParser parser1 = new QueryParser(Version.LUCENE_40, "filename", analyzer);
		           query1 = parser1.parse(keyword);

	           }
	           else{
	        	   QueryParser parser1 = new QueryParser(Version.LUCENE_40, "content", analyzer);	        	   query1 = parser1.parse(keyword);
	        	   query1 = parser1.parse(keyword);
	           } 
	            QueryParser parser2 = new QueryParser(Version.LUCENE_40, "type", analyzer);
	            Query query2 = parser2.parse(txt+" "+doc+" "+xls+" "+pdf+" "+ppt);
	            
	            BooleanQuery q=new BooleanQuery();
	            q.add(query1,Occur.MUST);
	            q.add(query2,Occur.MUST);
	            
	            QueryScorer score= new QueryScorer(query1);
	            Fragmenter fragmenter=new SimpleSpanFragmenter(score);
	            SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("<font color='red'>", "</font>");
	            Highlighter highlighter=new Highlighter(simpleHTMLFormatter,score);
	            highlighter.setTextFragmenter(fragmenter);
	            
	            ScoreDoc[] hits = isearcher.search(q, null, 1000).scoreDocs;
	            
	        	out.println(docType +
	        		    "<html>\n" +
	        		    "<head><title>" + title+ "</title></head>\n" +
	        		    "<body bgcolor=\"#f0f0f0\">\n" +
	        		    "<h1 align=\"center\">" + title+"\n"+"</h1>\n" +
	        		    "<ul>\n" +
	        		    "</ul>\n" +
	        		    "  <li><b>检索词</b>："
	        		    + keyword +"<br/>"+"<br/>"+"  <li><b>检索结果</b>："+hits.length+"<br/>"
	        		    +"</body></html>");
	        	
	            for (int i = 0; i < hits.length; i++) {
	                Document hitDoc = isearcher.doc(hits[i].doc);
	                
	                String text=hitDoc.get("filename");
	                String text1=text.substring(0,text.lastIndexOf("."));
	                String type=hitDoc.get("type");
	                String text2=hitDoc.get("content");
	                TokenStream tokenstream=analyzer.tokenStream("filename", new StringReader(text1));
//		            TokenStream tokenstream2=analyzer.tokenStream("content", new StringReader(text2));
		            String str1 = highlighter.getBestFragment(tokenstream, text1);
		            tokenstream=analyzer.tokenStream("content", new StringReader(text2));
		            String str2 = highlighter.getBestFragment(tokenstream, text2);
		            
	               if(str1==null)
	            	   str1=text1;
	               if(str2==null)
            	       str2="";
	               String path=hitDoc.get("path");
		           String newpath=path.substring(0,path.lastIndexOf("\\")+1)+str1+"."+type;
		            out.println(
	            			"<html>\n" +
	            			"<body bgcolor=\"#f0f0f0\">\n" +"<br/>"+
	            			"<font color='blue'>"+newpath+"</font>"+"<br/>"+
	            			"&nbsp&nbsp"+str2+"<br/>"+
	            			
	            			"</body></html>");
	            }
	            ireader.close();
	            directory.close();
	            out.flush();
	            out.close();
	        }catch(Exception e){
	            //e.printStackTrace();
	        	System.out.println(e.getMessage());
	            out.println(
            			"<html>\n" +
            			"<body bgcolor=\"#f0f0f0\">\n" +
            			"<h1 align=\"center\">" + "检索异常"+"\n"+"</h1>\n"+ 
            			"</body></html>");
	        }
	
	}
    protected void doPost(HttpServletRequest request,HttpServletResponse response)throws ServletException, IOException{
    	response.setContentType("text/html;charset=UTF-8");
		PrintWriter out = response.getWriter();
		String docType ="<!doctype html public \"-//w3c//dtd html 4.0 " +
				"transitional//en\">\n";
		String DATA_DIR=request.getParameter("DATA_DIR");
		INDEX_DIR=request.getParameter("INDEX_DIR");
		
		try{
			IndexManager.setpath(DATA_DIR, INDEX_DIR);
			File fileIndex = new File(INDEX_DIR);
            if(IndexManager.deleteDir(fileIndex)){
            fileIndex.mkdir();
           }else{
              fileIndex.mkdir();
           }
           List<File> fs=IndexManager.getFileList(INDEX_DIR);
           for (File file : fs)
        	   file.delete();
           IndexManager.createIndex(DATA_DIR);
           out.println(docType +
       		    "<html>\n" +
       		    "<head><title>" + "索引结果"+ "</title></head>\n" +
       		    "<body bgcolor=\"#f0f0f0\">\n" +
       		    "<h1 align=\"center\">" + "索引成功"+"\n"+"</h1>\n" +
       		    "<ul>\n" +
       		    "</ul>\n" +
       		    "  <li><b>文件路径</b>："
       		    + DATA_DIR +"<br/>"+"<br/>"+"  <li><b>索引路径</b>："+INDEX_DIR+"<br/>"
       		    +"</body></html>");
        }catch(Exception e){
            //e.printStackTrace();
        	System.out.println(e.getMessage());
            out.println(
        			"<html>\n" +
        			"<body bgcolor=\"#f0f0f0\">\n" +
        			"<h1 align=\"center\">" + "索引异常"+"\n"+"</h1>\n"+ 
        			"</body></html>");
        }
}
}