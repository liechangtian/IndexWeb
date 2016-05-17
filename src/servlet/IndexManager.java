/*���ز����ļ�*/

package servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;

import org.apache.pdfbox.io.RandomAccessRead;
import org.apache.pdfbox.pdfparser.PDFParser;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.*;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.hwpf.usermodel.Paragraph;
import org.apache.poi.hwpf.usermodel.Range;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xslf.XSLFSlideShow;
import org.apache.poi.xslf.extractor.XSLFPowerPointExtractor;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.xmlbeans.XmlException;
import org.openxmlformats.schemas.drawingml.x2006.main.CTRegularTextRun;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextBody;
import org.openxmlformats.schemas.drawingml.x2006.main.CTTextParagraph;
import org.openxmlformats.schemas.drawingml.x2006.spreadsheetDrawing.CTGroupShape;
import org.openxmlformats.schemas.presentationml.x2006.main.CTShape;
import org.openxmlformats.schemas.presentationml.x2006.main.CTSlide;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLTextExtractor;
import org.apache.poi.hslf.HSLFSlideShow;  
import org.apache.poi.hslf.model.Slide;  
import org.apache.poi.hslf.model.TextRun;  
import org.apache.poi.hslf.usermodel.SlideShow;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.openxmlformats.schemas.*;

public class IndexManager{
    private static IndexManager indexManager;
    private static String content="";
    
    private static String DATA_DIR = "C:\\LCT\\Work\\data\\LuceneData";
    private static String INDEX_DIR = "C:\\LCT\\Work\\data\\LuceneIndex";
    private static Analyzer analyzer = null;
    private static Directory directory = null;
    private static IndexWriter indexWriter = null;
    

    /**
     * ��������������
     * @return ������������������
     */
    public IndexManager getManager(){
        if(indexManager == null){
        	IndexManager.indexManager = new IndexManager();
        }
        return indexManager;
    }
    /**
     * ������ǰ�ļ�Ŀ¼������
     * @param path ��ǰ�ļ�Ŀ¼
     * @return �Ƿ�ɹ�
     * @throws Exception 
     */
    public static boolean createIndex(String path) throws Exception{
        Date date1 = new Date();
        List<File> fileList = getFileList(path);
        for (File file : fileList) {
            content = "";
            //��ȡ�ļ���׺
            String fileName=file.getName();
            String type = fileName.substring(fileName.lastIndexOf(".")+1);
            System.out.print(type+"\n");
            if("txt".equalsIgnoreCase(type)){
                
                content += readTxt(file);
            
            }else if("doc".equalsIgnoreCase(type)){
            
                content += readWord2003(file);
            
            }else if("docx".equalsIgnoreCase(type)){
            
                content += readWord2007(file);
                type="doc";
            
            }else if("xls".equalsIgnoreCase(type)){
                
                content += readExcel2003(file);
                
            }else if("xlsx".equalsIgnoreCase(type)){
                
                content += readExcel2007(file);
                type="xls";
            }else if("pdf".equalsIgnoreCase(type)){
                
                content += readPdf(file);
                
            }else if("ppt".equalsIgnoreCase(type)){
                
                content += readPPT2003(file);
                
            }else if("pptx".equalsIgnoreCase(type)){
                
                content += readPPT2007(file);
                type="ppt";
            }else if("html".equalsIgnoreCase(type)){
                
                content += readHtml(file);
                type="html";
            }
            System.out.print(type+"\n");

            System.out.println("name :"+file.getName());
            System.out.println("path :"+file.getPath());
            System.out.println("content :"+content);
            System.out.println();
            
            
            try{
                analyzer = new StandardAnalyzer(Version.LUCENE_40);
                directory = FSDirectory.open(new File(INDEX_DIR));
    
                File indexFile = new File(INDEX_DIR);
                if (!indexFile.exists()) {
                    indexFile.mkdirs();
                }
                IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
                indexWriter = new IndexWriter(directory, config);
                
                Document document = new Document();
                document.add(new TextField("filename", file.getName(), Store.YES));
                document.add(new TextField("content", content, Store.YES));
                document.add(new TextField("path", file.getPath(), Store.YES));
                document.add(new TextField("type", type, Store.YES));
                indexWriter.addDocument(document);
                indexWriter.commit();
                closeWriter();
    
                
            }catch(Exception e){
                e.printStackTrace();
            }
            content = "";
        }
      
        Date date2 = new Date();
        System.out.println("��������-----��ʱ��" + (date2.getTime() - date1.getTime()) + "ms\n");
        return true;
    }
    
    /**
     * �������������ط����������ļ�
     * @param text ���ҵ��ַ���
     * @return �����������ļ�List
     */
    public static void searchIndex(String text){
        Date date1 = new Date();
        try{
            directory = FSDirectory.open(new File(INDEX_DIR));
            analyzer = new StandardAnalyzer(Version.LUCENE_40);
            DirectoryReader ireader = DirectoryReader.open(directory);
            IndexSearcher isearcher = new IndexSearcher(ireader);
    
            QueryParser parser = new QueryParser(Version.LUCENE_40, "content", analyzer);
            Query query = parser.parse(text);
            
            ScoreDoc[] hits = isearcher.search(query, null, 1000).scoreDocs;
        
            for (int i = 0; i < hits.length; i++) {
                Document hitDoc = isearcher.doc(hits[i].doc);
                System.out.println("������������������������������������");
                System.out.println(hitDoc.get("filename"));
                System.out.println(hitDoc.get("content"));
                System.out.println(hitDoc.get("path"));
                System.out.println("����������������������������������������������������������������");
            }
            ireader.close();
            directory.close();
        }catch(Exception e){
            e.printStackTrace();
        }
        Date date2 = new Date();
        System.out.println("�鿴����-----��ʱ��" + (date2.getTime() - date1.getTime()) + "ms\n");
    }
    
    
    /**
     * ��ȡtxt�ļ�������
     * @param file ��Ҫ��ȡ���ļ�����
     * @return �����ļ�����
     */
    public static String readTxt(File file) throws IOException {  
        StringBuffer sb = new StringBuffer("");  
        InputStream is = new FileInputStream(file.getPath());  
        // �������ó�GBK�����򽫳�������  
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "GBK"));  
        try {  
            String line = "";  
            while ((line = reader.readLine()) != null) {  
                sb.append(line + "\r");  
            }  
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        }  
        return sb.toString().trim();  
    }  
       
    
    /**
     * ��ȡdoc�ļ�����
     * @param file ��Ҫ��ȡ���ļ�����
     * @return �����ļ�����
     * @throws IOException 
     */
    public static String readWord2003(File file) throws Exception {  
        String bodyText = null;  
        InputStream inputStream = new FileInputStream(file.getPath());  
        WordExtractor extractor = new WordExtractor(inputStream);   
        bodyText = extractor.getText();  
        extractor.close();
        return bodyText;  
    }  
    public static String  readWord2007(File file) throws IOException, OpenXML4JException, XmlException{
    	OPCPackage opcPackage = POIXMLDocument.openPackage(file.getPath());  
		POIXMLTextExtractor ex = new XWPFWordExtractor(opcPackage);  
		String bodyText=ex.getText();
		ex.close();
        return bodyText; 
    }

    
    /**
     * ��ȡxls�ļ�����
     * @param file ��Ҫ��ȡ���ļ�����
     * @return �����ļ�����
     */
    public static String readExcel2003(File file) throws IOException {  
        InputStream inputStream = null;  
        String content = null;  
        try {  
            inputStream = new FileInputStream(file.getPath());  
            HSSFWorkbook wb = new HSSFWorkbook(inputStream);  
            ExcelExtractor extractor = new ExcelExtractor(wb);  
            extractor.setFormulasNotResults(true);  
            extractor.setIncludeSheetNames(false);  
            content = extractor.getText();  
            extractor.close();
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        }  
        return content;  
    } 
    public static String readExcel2007(File file) throws IOException {  
        StringBuffer content = new StringBuffer();  
        // ���� XSSFWorkbook ����strPath �����ļ�·��  
        XSSFWorkbook xwb = new XSSFWorkbook(file.getPath());  
        // ѭ��������Sheet  
        for (int numSheet = 0; numSheet < xwb.getNumberOfSheets(); numSheet++) {  
            XSSFSheet xSheet = xwb.getSheetAt(numSheet);  
            if (xSheet == null) {  
                continue;  
            }  
            // ѭ����Row  
            for (int rowNum = 0; rowNum <= xSheet.getLastRowNum(); rowNum++) {  
                XSSFRow xRow = xSheet.getRow(rowNum);  
                if (xRow == null) {  
                    continue;  
                }  
                // ѭ����Cell  
                for (int cellNum = 0; cellNum <= xRow.getLastCellNum(); cellNum++) {  
                    XSSFCell xCell = xRow.getCell(cellNum);  
                    if (xCell == null) {  
                        continue;  
                    }  
                    if (xCell.getCellType() == XSSFCell.CELL_TYPE_BOOLEAN) {  
                        content.append(xCell.getBooleanCellValue());  
                    } else if (xCell.getCellType() == XSSFCell.CELL_TYPE_NUMERIC) {  
                        content.append(xCell.getNumericCellValue());  
                    } else {  
                        content.append(xCell.getStringCellValue());  
                    }  
                }  
            }  
        }  
        xwb.close();
        return content.toString();  
    }  

    /**
     * ��ȡppt�ļ�����
     * @param file ��Ҫ��ȡ���ļ�����
     * @return �����ļ�����
     */
    @SuppressWarnings("resource")
	public static String readPPT2007(File file) throws IOException, XmlException, OpenXML4JException {
        return new XSLFPowerPointExtractor(POIXMLDocument.openPackage(file.getPath())).getText();   
   }

    public static String readPPT2003(File file){
        String result = "";
        try{
        	StringBuffer content=new StringBuffer("");
	        FileInputStream fis=new FileInputStream(file);
	        SlideShow p = new SlideShow(new HSLFSlideShow(fis));;
	        Slide[] slides = p.getSlides();
	        for (int i = 0; i < slides.length; i++) {  
                TextRun[] t = slides[i].getTextRuns();// Ϊ��ȡ�ûõ�Ƭ���������ݣ�����TextRun  
                for (int j = 0; j < t.length; j++) {  
                    content.append(t[j].getText());// ����Ὣ�������ݼӵ�content��ȥ  
                }  
            }  
	        result+=content.toString();
        }catch (Exception ex) {  
            System.out.println(ex.toString());  
        }  
        return result;
    }
    
     
    /**
     * ��ȡpdf�ļ�����
     * @param file ��Ҫ��ȡ���ļ�����
     * @return �����ļ�����
     */
    public static String readPdf(File file) throws IOException {  
        StringBuffer content = new StringBuffer("");// �ĵ�����  
        PDDocument pdfDocument = null;  
        try {  
            FileInputStream fis = new FileInputStream(file);  
            PDFTextStripper stripper = new PDFTextStripper();  
            pdfDocument = PDDocument.load(fis);  
            StringWriter writer = new StringWriter();  
            stripper.writeText(pdfDocument, writer);  
            content.append(writer.getBuffer().toString());  
            fis.close();  
        } catch (java.io.IOException e) {  
            System.err.println("IOException=" + e);  
            System.exit(1);  
        } finally {  
            if (pdfDocument != null) {  
                org.apache.pdfbox.cos.COSDocument cos = pdfDocument.getDocument();  
                cos.close();  
                pdfDocument.close();  
            }  
        }  
        return content.toString();  
  
    }
  //��ȡHTML�ļ����ı�����
		private static String readHtml(File file) 
	    { 
	         
	              String textStr =""; 
	              Pattern p_script; 
	              Matcher m_script; 
	              Pattern p_style; 
	              Matcher m_style; 
	              Pattern p_html; 
	              Matcher m_html;
	              Pattern p_houhtml; 
	              Matcher m_houhtml;
	              Pattern p_spe; 
	              Matcher m_spe;
	              Pattern p_blank; 
	              Matcher m_blank;
	              Pattern p_table; 
	              Matcher m_table;
	              Pattern p_enter; 
	              Matcher m_enter;
	           
	              try { 
	               String regEx_script = "<[\\s]*?script[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?script[\\s]*?>"; 
	               //����script��������ʽ.
	               String regEx_style = "<[\\s]*?style[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?style[\\s]*?>"; 
	               //����style��������ʽ. 
	               String regEx_html = "<[^>]+>"; 
	               //����HTML��ǩ��������ʽ 
	               String regEx_houhtml = "/[^>]+>"; 
	               //����HTML��ǩ��������ʽ 
	               String regEx_spe="\\&[^;]+;";
	               //����������ŵ�������ʽ
	               String regEx_blank=" +";
	               //�������ո��������ʽ
	               String regEx_table="\t+";
	               //�������Ʊ����������ʽ
	               String regEx_enter="\n+";
	               //�������س���������ʽ
	             
	               BufferedReader br = new BufferedReader(new FileReader(file));//����һ��BufferedReader������ȡ�ļ�
	  	        String s = null;
	  	        String htmlStr = "";
	            while((s = br.readLine())!=null){//ʹ��readLine������һ�ζ�һ��
	            	htmlStr = htmlStr + "\n" +s;
	            }
	            br.close(); 
	               p_script = Pattern.compile(regEx_script,Pattern.CASE_INSENSITIVE); 
	               m_script = p_script.matcher(htmlStr); 
	               htmlStr = m_script.replaceAll(""); //����script��ǩ

	               p_style = Pattern.compile(regEx_style,Pattern.CASE_INSENSITIVE); 
	               m_style = p_style.matcher(htmlStr); 
	               htmlStr = m_style.replaceAll(""); //����style��ǩ 
	              
	               p_html = Pattern.compile(regEx_html,Pattern.CASE_INSENSITIVE); 
	               m_html = p_html.matcher(htmlStr); 
	               htmlStr = m_html.replaceAll(""); //����html��ǩ 
	               
	               p_houhtml = Pattern.compile(regEx_houhtml,Pattern.CASE_INSENSITIVE); 
	               m_houhtml = p_houhtml.matcher(htmlStr); 
	               htmlStr = m_houhtml.replaceAll(""); //����html��ǩ 
	               
	               p_spe = Pattern.compile(regEx_spe,Pattern.CASE_INSENSITIVE); 
	               m_spe = p_spe.matcher(htmlStr); 
	               htmlStr = m_spe.replaceAll(""); //����������� 
	               
	               p_blank = Pattern.compile(regEx_blank,Pattern.CASE_INSENSITIVE); 
	               m_blank = p_blank.matcher(htmlStr); 
	               htmlStr = m_blank.replaceAll(" "); //���˹���Ŀո�
	               
	               p_table = Pattern.compile(regEx_table,Pattern.CASE_INSENSITIVE); 
	               m_table = p_table.matcher(htmlStr); 
	               htmlStr = m_table.replaceAll(" "); //���˹�����Ʊ��

	               p_enter = Pattern.compile(regEx_enter,Pattern.CASE_INSENSITIVE); 
	               m_enter = p_enter.matcher(htmlStr); 
	               htmlStr = m_enter.replaceAll(" "); //���˹�����Ʊ��
	               
	               textStr = htmlStr; 
	              
	              }catch(Exception e) 
	              { 
	                    System.err.println("Html2Text: " + e.getMessage()); 
	              } 
	           
	              return textStr;//�����ı��ַ��� 
	    }
    
   
    
    
   
    /**
     * ����Ŀ¼�µ��ļ�
     * @param dirPath ��Ҫ��ȡ�ļ���Ŀ¼
     * @return �����ļ�list
     */
    public static List<File> getFileList(String dirPath) {
        File[] files = new File(dirPath).listFiles();
        List<File> fileList = new ArrayList<File>();
        for (File file : files) {
            if (isTarFile(file.getName())) {
                fileList.add(file);
            }
        }
        return fileList;
    }
    /**
     * �ж��Ƿ�ΪĿ���ļ���Ŀǰ֧��txt xls doc��ʽ
     * @param fileName �ļ�����
     * @return ������ļ����������������������true�����򷵻�false
     */
    public static boolean isTarFile(String fileName) {
        if (fileName.lastIndexOf(".txt") > 0) {
            return true;
        }else if (fileName.lastIndexOf(".xls") > 0) {
            return true;
        }else if (fileName.lastIndexOf(".xlsx") > 0) {
            return true;
        }else if (fileName.lastIndexOf(".doc") > 0) {
            return true;
        }else if (fileName.lastIndexOf(".docx") > 0) {
            return true;
        }else if (fileName.lastIndexOf(".pdf") > 0) {
            return true;
        }else if (fileName.lastIndexOf(".ppt") > 0) {
            return true;
        }else if (fileName.lastIndexOf(".pptx") > 0) {
            return true;
        }else if (fileName.lastIndexOf(".html") > 0) {
            return true;
        }
        else return false;
    }
    
    public static void closeWriter() throws Exception {
        if (indexWriter != null) {
            indexWriter.close();
        }
    }
    /**
     * ɾ���ļ�Ŀ¼�µ������ļ�
     * @param file Ҫɾ�����ļ�Ŀ¼
     * @return ����ɹ�������true.
     */
    public static boolean deleteDir(File file){
        if(file.isDirectory()){
            File[] files = file.listFiles();
            for(int i=0; i<files.length; i++){
                deleteDir(files[i]);
            }
        }
        file.delete();
        return true;
    }
    
    public static void setpath(String a,String b){
    	DATA_DIR=a;
    	INDEX_DIR=b;
    }
    public static void main(String[] args) throws Exception{
        File fileIndex = new File(INDEX_DIR);
        if(deleteDir(fileIndex)){
            fileIndex.mkdir();
        }else{
            fileIndex.mkdir();
        }
        List<File> fs=getFileList(INDEX_DIR);
        for (File file : fs)
        	file.delete();
        createIndex(DATA_DIR);
    }
}