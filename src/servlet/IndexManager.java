/*���ز����ļ�*/

package servlet;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.util.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.TextField;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.Version;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.openxml4j.exceptions.OpenXML4JException;
import org.apache.poi.openxml4j.opc.OPCPackage;
import org.apache.poi.xslf.extractor.XSLFPowerPointExtractor;
import org.apache.poi.xssf.usermodel.XSSFCell;
import org.apache.poi.xssf.usermodel.XSSFRow;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.xmlbeans.XmlException;
import org.apache.poi.POIXMLDocument;
import org.apache.poi.POIXMLTextExtractor;
import org.apache.poi.hslf.HSLFSlideShow;  
import org.apache.poi.hslf.model.Slide;  
import org.apache.poi.hslf.model.TextRun;  
import org.apache.poi.hslf.usermodel.SlideShow;
import org.apache.poi.hssf.extractor.ExcelExtractor;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

public class IndexManager{
    private static IndexManager indexManager;
    private static String content="";//�洢�ַ���������ļ�����
    private static String DATA_DIR = "";//�ļ����·��
    private static String INDEX_DIR = "";//���������ļ��Ĵ洢·��
    private static Analyzer analyzer = null;//�﷨������
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
        analyzer = new StandardAnalyzer(Version.LUCENE_40);//��ʼ���ʷ�������
        directory = FSDirectory.open(new File(INDEX_DIR));//�������·��

        File indexFile = new File(INDEX_DIR);
        if (!indexFile.exists()) {
            indexFile.mkdirs();
        }
        //����indexWriter������ΪLucene�汾�ͷ�����
        IndexWriterConfig config = new IndexWriterConfig(Version.LUCENE_40, analyzer);
        indexWriter = new IndexWriter(directory, config);
        
        List<File> fileList = getFileList(path);
        for (File file : fileList) {
            content = "";
           
            //��ȡ�ļ����ͣ��������ļ����͵��ò�ͬ�ķ�����ȡ�ļ�
            String fileName=file.getName();
            String type = fileName.substring(fileName.lastIndexOf(".")+1);
            switch(type){
            case "txt":
            	content += readTxt(file);
            	break;
            case "doc":
            	content += readWord2003(file);
            	break;
            case "docx":
            	content += readWord2007(file);
            	type="doc";//���ʹ洢Ϊdoc
            	break;
            case "xls":
            	content += readExcel2003(file);
            	break;
            case "xlsx":
            	content += readExcel2007(file);
            	type="xls";//���ʹ洢Ϊxls
            	break;
            case "pdf":
            	content += readPdf(file);
            	break;
            case "ppt":
            	content += readPPT2003(file);
            	break;
            case "pptx":
            	content += readPPT2007(file);
            	type="ppt";//���ʹ洢Ϊppt
            	break;
            case "htm":
            	content += readHtml(file);
            	type="html";//���ʹ洢Ϊhtml
            	break;
            case "html":
            	content += readHtml(file);
            	break;
            default:type="";
            }

            //����document���ֱ�������������ļ������ļ����ݡ��ļ�·�����ļ����ͣ�������2003��2007�汾����Ȼ��ͨ��indexWriter��document����д��������
            Document document = new Document();
            document.add(new TextField("filename", file.getName(), Store.YES));
            document.add(new TextField("content", content, Store.YES));
            document.add(new TextField("path", file.getPath(), Store.YES));
            document.add(new TextField("type", type, Store.YES));
            indexWriter.addDocument(document);
            indexWriter.commit();
            content = "";
        }
        closeWriter();//�ر�indexWriter
        return true;
    }
    
    private static void closeWriter() throws Exception {
        if (indexWriter != null) {
            indexWriter.close();
        }
    }
    /**
     * ��ȡtxt�ļ�������
     * @param file ��Ҫ��ȡ���ļ�����
     * @return �����ļ�����
     */
    private static String readTxt(File file) throws IOException {  
        StringBuffer sb = new StringBuffer("");  
        InputStream is = new FileInputStream(file.getPath());  
        // �������ó�GBK�����򽫳�������  
        BufferedReader reader = new BufferedReader(new InputStreamReader(is, "GBK"));  
        //���ж�������
        try {  
            String line = "";  
            while ((line = reader.readLine()) != null) {  
                sb.append(line + "\r");  
            }  
        } catch (FileNotFoundException e) {  
            e.printStackTrace();  
        }  
        return sb.toString().trim();  //��sbת��Ϊ�ַ���������
    }  
       
    
    /**
     * ��ȡdoc�ļ�����
     * @param file ��Ҫ��ȡ���ļ�����
     * @return �����ļ�����
     * @throws IOException 
     */
    private static String readWord2003(File file) throws Exception {  
        String bodyText = null;  
        InputStream inputStream = new FileInputStream(file.getPath());  
        WordExtractor extractor = new WordExtractor(inputStream);   
        bodyText = extractor.getText();  
        extractor.close();
        return bodyText;  
    }  
    private static String  readWord2007(File file) throws IOException, OpenXML4JException, XmlException{
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
    private static String readExcel2003(File file) throws IOException {  
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
    private static String readExcel2007(File file) throws IOException {  
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
    private static String readPPT2003(File file){
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
    @SuppressWarnings("resource")
    private static String readPPT2007(File file) throws IOException, XmlException, OpenXML4JException {
        return new XSLFPowerPointExtractor(POIXMLDocument.openPackage(file.getPath())).getText();   
   }

  
    
     
    /**
     * ��ȡpdf�ļ�����
     * @param file ��Ҫ��ȡ���ļ�����
     * @return �����ļ�����
     */
    private static String readPdf(File file) throws IOException {  
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
    
    
    /**
     * ��ȡhtm��html�ļ�����
     * @param file ��Ҫ��ȡ���ļ�����
     * @return �����ļ�����
     */
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
	               //����script��������ʽ.
	               String regEx_script = "<[\\s]*?script[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?script[\\s]*?>"; 
	               //����style��������ʽ. 
	               String regEx_style = "<[\\s]*?style[^>]*?>[\\s\\S]*?<[\\s]*?\\/[\\s]*?style[\\s]*?>"; 
	               //����HTML��ǩ��������ʽ 
	               String regEx_html = "<[^>]+>"; 
	               //����HTML��ǩ��������ʽ 
	               String regEx_houhtml = "/[^>]+>"; 
	               //����������ŵ�������ʽ
	               String regEx_spe="\\&[^;]+;";
	               //�������ո��������ʽ
	               String regEx_blank=" +";
	               //�������Ʊ����������ʽ
	               String regEx_table="\t+";
	               //�������س���������ʽ
	               String regEx_enter="\n+";
	             
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
     * �ж��Ƿ�ΪĿ���ļ���Ŀǰ֧��txt xls doc pdf ppt html�ȸ�ʽ
     * @param fileName �ļ�����
     * @return ������ļ����������������������true�����򷵻�false
     */
    private static boolean isTarFile(String fileName) {
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
        }else if (fileName.lastIndexOf(".htm") > 0) {
            return true;
        }else return false;
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
        deleteDir(fileIndex);
            fileIndex.mkdirs();
//        }else{
//            fileIndex.mkdir();
//        }
//        List<File> fs=getFileList(INDEX_DIR);
//        for (File file : fs){
//        	 System.out.println("fgsagfsgs");
//        	file.delete();
//        }
        createIndex(DATA_DIR);
    }
}