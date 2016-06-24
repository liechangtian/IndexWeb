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
		
		String field=request.getParameter("field");//提取搜索范围
		String keyword=(request.getParameter("kw"));//提取搜索语句
		String freshflag=(request.getParameter("freshflag"));//提取搜索语句
		DATA_DIR=request.getParameter("DATA_DIR");//提取文件路径
		INDEX_DIR=request.getParameter("INDEX_DIR");//提取索引路径
		
		//提取文件类型
		String txt=(request.getParameter("txt"));
		String doc=(request.getParameter("doc"));
		String xls=(request.getParameter("xls"));
		String pdf=(request.getParameter("pdf"));
		String ppt=(request.getParameter("ppt"));
		String html=(request.getParameter("html"));
		
		try{
			//检测刷新标识，若不为空，则建立索引，否则直接搜索关键词
			if(freshflag!=null){
				String docType ="<!doctype html public \"-//w3c//dtd html 4.0 " +
						"transitional//en\">\n";
				IndexManager.setpath(DATA_DIR, INDEX_DIR);//初始化文件路径和索引路径
				File fileIndex = new File(INDEX_DIR);
	            IndexManager.deleteDir(fileIndex);
	            fileIndex.mkdir();
	           IndexManager.createIndex(DATA_DIR);//对DATA_DIR下的文件在INDEX_DIR下建立索引
	           
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
			}
			//处理搜索请求
			else{ 
				directory = FSDirectory.open(new File(INDEX_DIR));//打开索引存放路径
	            analyzer = new StandardAnalyzer(Version.LUCENE_40);//初始化词法分析器
	            DirectoryReader ireader = DirectoryReader.open(directory);
	            IndexSearcher isearcher = new IndexSearcher(ireader);//创建搜索器
	    
	           //判断搜索范围，根据不同的搜索范围生成不同的query1
	            Query query1;
	           if(field.equals("all")){
	        	   String[] fields={"filename","content"};
	        	   MultiFieldQueryParser mparser=new MultiFieldQueryParser(Version.LUCENE_40, fields, analyzer);//多域搜索语法解析器
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
	           //根据提取的文件类型生成query2
	            QueryParser parser2 = new QueryParser(Version.LUCENE_40, "type", analyzer);
	            Query query2 = parser2.parse(txt+" "+doc+" "+xls+" "+pdf+" "+ppt+" "+html);
	           //合并 query1和query2
	            BooleanQuery q=new BooleanQuery();
	            q.add(query1,Occur.MUST);
	            q.add(query2,Occur.MUST);
	            //设置高亮显示
	            QueryScorer score= new QueryScorer(query1);
	            Fragmenter fragmenter=new SimpleSpanFragmenter(score);
	            SimpleHTMLFormatter simpleHTMLFormatter = new SimpleHTMLFormatter("", "");
	            Highlighter highlighter=new Highlighter(simpleHTMLFormatter,score);
	            highlighter.setTextFragmenter(fragmenter);
	            
	            ScoreDoc[] hits = isearcher.search(q, null, 1000).scoreDocs;//搜索结果集合
	            JSONObject kword = new JSONObject();
	            JSONObject number = new JSONObject();
	    		kword.put("keyword", keyword);
	    		number.put("number", hits.length);
	    		//返回关键词与命中结果数
	        	out.write(kword.toString());
	        	out.write(number.toString());
	           
	        	for (int i = 0; i < hits.length; i++) {
	                Document hitDoc = isearcher.doc(hits[i].doc);
	                //提取文件名，滤掉后缀
	                String filename=hitDoc.get("filename");
	                String text1=filename.substring(0,filename.lastIndexOf("."));
	               //提取文件内容
	                String text2=hitDoc.get("content");
	               //对文件名进行高亮预处理
	                TokenStream tokenstream=analyzer.tokenStream("filename", new StringReader(text1));
		            String str1 = highlighter.getBestFragment(tokenstream, text1);
		            //对文件内容进行高亮预处理
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
		            //返回搜索结果 
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
            			"<h1 align=\"center\">" + "检索异常"+"\n"+"</h1>\n"+ 
            			"</body></html>");
	        }
	
	}
}