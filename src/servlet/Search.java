package servlet;

import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
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
		String keyword=(request.getParameter("kw"));
		String range=(request.getParameter("range"));
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
	    
	            QueryParser parser1 = new QueryParser(Version.LUCENE_40, range, analyzer);
	            Query query1 = parser1.parse(keyword);
	            QueryParser parser2 = new QueryParser(Version.LUCENE_40, "type", analyzer);
	            Query query2 = parser2.parse(txt+" "+doc+" "+xls+" "+pdf+" "+ppt);
//	            Term t1=new Term(range,keyword);
//	            TermQuery q1=new TermQuery(t1);
//	            Term t2=new Term("type",xls);
//	            TermQuery q2=new TermQuery(t2);
	            
	            BooleanQuery q=new BooleanQuery();
	            q.add(query1,Occur.MUST);
	            q.add(query2,Occur.MUST);
	            
	            ScoreDoc[] hits = isearcher.search(q, null, 1000).scoreDocs;
	            
	        	out.println(docType +
	        		    "<html>\n" +
	        		    "<head><title>" + title+ "</title></head>\n" +
	        		    "<body bgcolor=\"#f0f0f0\">\n" +
	        		    "<h1 align=\"center\">" + title+"\n"+"</h1>\n" +
	        		    "<ul>\n" +
	        		    "</ul>\n" +
	        		    "  <li><b>检索词</b>："
	        		    + keyword +"<br/>"+"<br/>"+"  <li><b>检索结果</b>："+"<br/>"
	        		    +"</body></html>");
	            for (int i = 0; i < hits.length; i++) {
	                Document hitDoc = isearcher.doc(hits[i].doc);
	                out.println(
	            			"<html>\n" +
	            			"<body bgcolor=\"#f0f0f0\">\n" +"<br/>"+
	            			hitDoc.get("type")+"<br/>"+
	            			hitDoc.get("filename")+"<br/>"+
	            			hitDoc.get("content")+"<br/>"+
	            			hitDoc.get("path")+"<br/>"+
	            			
	            			"</body></html>");
	            }
	            ireader.close();
	            directory.close();
	            
	        }catch(Exception e){
	            e.printStackTrace();
	            out.println(
            			"<html>\n" +
            			"<body bgcolor=\"#f0f0f0\">\n" +
            			"<h1 align=\"center\">" + "检索异常"+"\n"+"</h1>\n"+ 
            			"</body></html>");
	        }
	
	}

}