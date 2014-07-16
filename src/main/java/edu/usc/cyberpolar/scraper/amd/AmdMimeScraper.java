package edu.usc.cyberpolar.scraper;

import org.apache.tika.sax.LinkContentHandler;
import org.apache.tika.sax.BodyContentHandler;
import java.net.URL;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.sax.Link;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.util.List;
import java.util.Arrays;
import java.util.Map;
import java.util.Vector;
import java.util.HashMap;

public class AmdMimeScraper{

    private Map<String, Integer> formatCounts = new HashMap<String, Integer>();  

    public void scrape(Map<String, String> replaceGroup, boolean trim, boolean lowerCase){
        try{
            String urlBase = "http://gcmd.gsfc.nasa.gov/KeywordSearch/";
	    URL url = new URL(urlBase+"Keywords.do?Portal=amd&KeywordPath=Parameters%7CCRYOSPHERE%7CSNOW%2FICE%7CSNOW%2FICE+TEMPERATURE&MetadataType=0&lbnode=mdlb4");
	    Parser parser = new AutoDetectParser();
	    LinkContentHandler handler = new LinkContentHandler();
	    Metadata met = new Metadata();
            parser.parse(url.openStream(), handler, met, new ParseContext());
	    for(Link link: handler.getLinks()){
		if(link.getUri().startsWith("Metadata")){
		    String nextUrl = urlBase+link.getUri();
		    BodyContentHandler bodyHandler = new BodyContentHandler();
		    parser.parse(new URL(nextUrl).openStream(), bodyHandler, met, new ParseContext());
		    String content = bodyHandler.toString();
		    String regex = "Distribution Format\\:\\s*([\\w, ?]*)";
		    Pattern pattern = Pattern.compile(regex);
		    Matcher matcher = pattern.matcher(content);
		    //System.out.println("URL: "+nextUrl);
		    List<String> formats = new Vector<String>();
		    if (matcher.find()){
		        formats = Arrays.asList(matcher.group(1).split(","));
			formats = cleanse(formats, replaceGroup, trim, lowerCase);
		    }
		    else{
			formats.add("unknown");
		    }

		    for(String format: formats){
			if(formatCounts.containsKey(format)){
			    int count = formatCounts.get(format);
			    count++;
			    formatCounts.put(format, count);
			}
			else{
			    formatCounts.put(format, 1);
			}
		    }
		    
		}
	    }

	    StringBuffer keyBuf = new StringBuffer();
	    for(String key: formatCounts.keySet()){
		keyBuf.append(key);
		keyBuf.append(",");
	    }
	    System.out.println(keyBuf.toString());
	    
	    StringBuffer valBuf = new StringBuffer();
	    for(String key: formatCounts.keySet()){	    
		valBuf.append(formatCounts.get(key));
		valBuf.append(",");
	    }
	    System.out.println(valBuf.toString());

	}
        catch(Exception e){
	    e.printStackTrace();
	    return;
	}

    }


    public List<String> cleanse(List<String> formats, Map<String, String> replaceGroup, boolean trim, boolean lowerCase){
	List<String> cleansedFormats = new Vector<String>(formats.size());
	for(String format: formats){
	    String cleansedFormat = format;
	    if(trim) cleansedFormat = cleansedFormat.trim();
	    if(lowerCase) cleansedFormat = cleansedFormat.toLowerCase();
	    boolean dontAdd=false;
	    for(String groupKey: replaceGroup.keySet()){
		if(cleansedFormat.indexOf(groupKey) != -1){
		    cleansedFormats.add(replaceGroup.get(groupKey));
		    dontAdd=true;
		}
	    }

	    if (!dontAdd) cleansedFormats.add(cleansedFormat);

	}
	return cleansedFormats;
    }

    

    public static void main(String [] args) throws Exception{
	String usage = "AmdMimeScraper <replace|with:replace|with..> <trim> <lower case>>\n";
	/* ascii|ascii:ascii|text:excel|excel:word|word: */
	AmdMimeScraper scraper = new AmdMimeScraper();
	if(args.length != 3){
	    System.err.println(usage);
	    System.exit(1);
	}

	String replaceGroupString = args[0];
	Map<String, String> replaceGroup = new HashMap<String, String>();
	String[] groups = replaceGroupString.split("\\:");
	for(int i=0; i < groups.length; i++){
	    String[] groupSpec = groups[i].split("\\|");
	    replaceGroup.put(groupSpec[0], groupSpec[1]);
	}
	boolean trim = Boolean.parseBoolean(args[1]);
	boolean lowerCase = Boolean.parseBoolean(args[2]);
        scraper.scrape(replaceGroup, trim, lowerCase);

    }

}