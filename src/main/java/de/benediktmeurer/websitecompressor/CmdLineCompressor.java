package de.benediktmeurer.websitecompressor;

/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import jargs.gnu.CmdLineParser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.Charset;

import org.mozilla.javascript.ErrorReporter;
import org.mozilla.javascript.EvaluatorException;

import com.googlecode.htmlcompressor.compressor.HtmlCompressor;
import com.googlecode.htmlcompressor.compressor.XmlCompressor;
import com.yahoo.platform.yui.compressor.CssCompressor;
import com.yahoo.platform.yui.compressor.JavaScriptCompressor;

/**
 * Wrapper for website compression tools <code>htmlcompressor</code>
 * and <code>yuicompressor</code>.
 * 
 * <p>Usage: <code>java -jar websitecompressor.jar [options] [files]</code></p>
 * <p>To view a list of all available parameters please run with <code>--help</code> option:</p>
 * <p><code>java -jar websitecompressor.jar --help</code></p>
 * 
 * @author <a href="mailto:benedikt.meurer@googlemail.com">Benedikt Meurer</a>
 */
public class CmdLineCompressor {
	
	private static CmdLineParser parser = new CmdLineParser();
	
	private static CmdLineParser.Option helpOpt;
	private static CmdLineParser.Option charsetOpt;
	private static CmdLineParser.Option compressCssOpt;
	private static CmdLineParser.Option compressJsOpt;
	private static CmdLineParser.Option disableOptimizationsOpt;
	private static CmdLineParser.Option lineBreakOpt;
	private static CmdLineParser.Option nomungeOpt;
	private static CmdLineParser.Option preserveCommentsOpt;
	private static CmdLineParser.Option preserveIntertagSpacesOpt;
	private static CmdLineParser.Option preserveLineBreaksOpt;
	private static CmdLineParser.Option preserveMultiSpacesOpt;
	private static CmdLineParser.Option preserveQuotesOpt;
	private static CmdLineParser.Option preserveSemiOpt;
	
	private static String charset;
	private static int lineBreakPos = -1;

	public static void main(String[] args) {
		helpOpt = parser.addBooleanOption('h', "help");
		charsetOpt = parser.addStringOption("charset");
		compressCssOpt = parser.addBooleanOption("compress-css");
		compressJsOpt = parser.addBooleanOption("compress-js");
		disableOptimizationsOpt = parser.addBooleanOption("disable-optimizations");
		lineBreakOpt = parser.addStringOption("line-break");
		nomungeOpt = parser.addBooleanOption("nomunge");
		preserveCommentsOpt = parser.addBooleanOption("preserve-comments");
		preserveIntertagSpacesOpt = parser.addBooleanOption("preserve-intertag-spaces");
		preserveLineBreaksOpt = parser.addBooleanOption("preserve-line-breaks");
		preserveMultiSpacesOpt = parser.addBooleanOption("preserve-multi-spaces");
		preserveQuotesOpt = parser.addBooleanOption("preserve-quotes");
		preserveSemiOpt = parser.addBooleanOption("preserve-semi");

		try {
			parser.parse(args);
		
			// help
			Boolean help = (Boolean)parser.getOptionValue(helpOpt);
			if (help != null && help.booleanValue()) {
				printUsage(System.out);
				System.exit(0);
			}
			
			// charset
			charset = (String)parser.getOptionValue(charsetOpt, "UTF-8");
			if (charset == null || !Charset.isSupported(charset)) {
				charset = "UTF-8";
			}
			
			// line-break
			String lineBreakStr = (String)parser.getOptionValue(lineBreakOpt);
			if (lineBreakStr != null) {
				try {
					lineBreakPos = Integer.parseInt(lineBreakStr, 10);
				}
				catch (NumberFormatException e) {
					printUsage(System.err);
					System.exit(1);
				}
			}
			
			// Compress all files and folders
			String[] fileNames = parser.getRemainingArgs();
			if (fileNames.length == 0) {
				printUsage(System.err);
				System.exit(1);
			}
			for (String fileName : fileNames) {
				File file = new File(fileName);
				compress(file);
			}
		}
		catch (NoClassDefFoundError e) {
			System.err.println(""
					+ "ERROR: The additional jar files called htmlcompressor-1.4.jar and\n"
					+ "yuicompressor-2.4.6.jar must be present in the same directory as\n"
					+ "the websitecompressor jar file.");
			System.exit(1);
		}
		catch (CmdLineParser.OptionException e) {
			printUsage(System.err);
			System.exit(1);
		}
		catch (Exception e) {
			e.printStackTrace(System.err);
			System.exit(1);
		}

		System.exit(0);
	}
	
	private static class CompressorCss implements Compressor {
		public String compress(String input) throws IOException {
			CssCompressor cssCompressor = new CssCompressor(new StringReader(input));
			StringWriter writer = new StringWriter();
			cssCompressor.compress(writer, lineBreakPos);
			return writer.toString();
		}
	}
	
	private static class CompressorHtml implements Compressor {
		private HtmlCompressor htmlCompressor = new HtmlCompressor();
		public CompressorHtml() {
			htmlCompressor.setCompressCss(parser.getOptionValue(compressCssOpt) != null);
			htmlCompressor.setCompressJavaScript(parser.getOptionValue(compressJsOpt) != null);
			htmlCompressor.setRemoveComments(parser.getOptionValue(preserveCommentsOpt) == null);
			htmlCompressor.setRemoveIntertagSpaces(parser.getOptionValue(preserveIntertagSpacesOpt) == null);
			htmlCompressor.setPreserveLineBreaks(parser.getOptionValue(preserveLineBreaksOpt) != null);
			htmlCompressor.setRemoveMultiSpaces(parser.getOptionValue(preserveMultiSpacesOpt) == null);
			htmlCompressor.setRemoveQuotes(parser.getOptionValue(preserveQuotesOpt) == null);
			htmlCompressor.setYuiCssLineBreak(lineBreakPos);
			htmlCompressor.setYuiJsDisableOptimizations(parser.getOptionValue(disableOptimizationsOpt) != null);
			htmlCompressor.setYuiJsLineBreak(lineBreakPos);
			htmlCompressor.setYuiJsNoMunge(parser.getOptionValue(nomungeOpt) != null);
			htmlCompressor.setYuiJsPreserveAllSemiColons(parser.getOptionValue(preserveSemiOpt) != null);
		}

		public String compress(String input) throws IOException {
			return htmlCompressor.compress(input);
		}
	}
	
	private static class CompressorJs implements Compressor {
		public String compress(String input) throws IOException {
			JavaScriptCompressor javaScriptCompressor = new JavaScriptCompressor(new StringReader(input), new ErrorReporter() {
                public void warning(String message, String sourceName, int line, String lineSource, int lineOffset) {
                    if (line < 0) {
                        System.err.println("\n[WARNING] " + message);
                    } else {
                        System.err.println("\n[WARNING] " + line + ':' + lineOffset + ':' + message);
                    }
                }

                public void error(String message, String sourceName, int line, String lineSource, int lineOffset) {
                    if (line < 0) {
                        System.err.println("\n[ERROR] " + message);
                    } else {
                        System.err.println("\n[ERROR] " + line + ':' + lineOffset + ':' + message);
                    }
                }

                public EvaluatorException runtimeError(String message, String sourceName, int line, String lineSource, int lineOffset) {
                    error(message, sourceName, line, lineSource, lineOffset);
                    return new EvaluatorException(message);
                }
            });
			boolean nomunge = (parser.getOptionValue(nomungeOpt) != null);
			boolean preserveSemi = (parser.getOptionValue(preserveSemiOpt) != null);
			boolean disableOptimizations = (parser.getOptionValue(disableOptimizationsOpt) != null);
			StringWriter writer = new StringWriter();
			javaScriptCompressor.compress(writer, lineBreakPos, !nomunge, false, preserveSemi, disableOptimizations);
			return writer.toString();
		}
	}
	
	private static class CompressorXml implements Compressor {
		private XmlCompressor xmlCompressor = new XmlCompressor();
		public CompressorXml() {
			xmlCompressor.setRemoveComments(parser.getOptionValue(preserveCommentsOpt) == null);
			xmlCompressor.setRemoveIntertagSpaces(parser.getOptionValue(preserveIntertagSpacesOpt) == null);
		}
		
		public String compress(String input) throws IOException {
			return xmlCompressor.compress(input);
		}
	}
	
	private static CompressorCss compressorCss = null;
	private static CompressorHtml compressorHtml = null;
	private static CompressorJs compressorJs = null;
	private static CompressorXml compressorXml = null;
	private static void compress(File file) throws IOException {
		if (file.isDirectory()) {
			File[] files = file.listFiles();
			if (files != null) {
				for (int i = 0; i < files.length; ++i) {
					compress(files[i]);
				}
			}
		}
		else {
			String fileName = file.getName();
			int fileDotIndex = fileName.lastIndexOf('.');
			if (fileDotIndex > 0 && fileDotIndex + 1 < fileName.length()) {
				String fileExtension = fileName.substring(fileDotIndex + 1);
				Compressor compressor = null;
				if ("css".equalsIgnoreCase(fileExtension)) {
					if (compressorCss == null) {
						compressorCss = new CompressorCss();
					}
					compressor = compressorCss;
				}
				else if ("html".equalsIgnoreCase(fileExtension)) {
					if (compressorHtml == null) {
						compressorHtml = new CompressorHtml();
					}
					compressor = compressorHtml;
				}
				else if ("js".equalsIgnoreCase(fileExtension)) {
					if (compressorJs == null) {
						compressorJs = new CompressorJs();
					}
					compressor = compressorJs;
				}
				else if ("xml".equalsIgnoreCase(fileExtension)) {
					if (compressorXml == null) {
						compressorXml = new CompressorXml();
					}
					compressor = compressorXml;
				}
				if (compressor != null) {
					// Read the input from the file
					StringBuilder input = new StringBuilder();
					InputStream fileInputStream = new FileInputStream(file);
					BufferedReader reader = new BufferedReader(new InputStreamReader(fileInputStream, charset));
					try {
						String line;
						String lineSeparator = System.getProperty("line.separator");
						while ((line = reader.readLine()) != null) {
							input.append(line);
							input.append(lineSeparator);
						}
					}
					finally {
						reader.close();
					}
					
					// Compress the file content
					String output = compressor.compress(input.toString());
					
					// Write the output to the file
					OutputStreamWriter writer = new OutputStreamWriter(new FileOutputStream(file), charset);
					try {
						writer.write(output);
					}
					finally {
						writer.close();
					}
				}
			}
		}
	}
	
	private static void printUsage(PrintStream s) {
		s.println(""
				+ "Usage: java -jar websitecompressor.jar [options] [files]\n"
				+ "\n"
				+ "<files or folders>          The files are compressed in-place\n"
				+ "\n"
				+ "Global Options:\n"
				+ " --charset <charset>        Read the input files using <charset>\n"
				+ " -h, --help                 Print this screen\n"
				+ "\n"
				+ "CSS Compression Options:\n"
				+ " --line-break <column>      Insert a line break after the specified column number\n"
				+ "\n"
				+ "HTML Compression Options:\n"
				+ " --compress-css             Enable inline CSS compression\n"
				+ " --compress-js              Enable inline JavaScript compression\n"
				+ " --preserve-comments        Preserve comments\n"
				+ " --preserve-intertag-spaces Preserve intertag spaces\n"
				+ " --preserve-line-breaks     Preserve line breaks\n"
				+ " --preserve-multi-spaces    Preserve multiple spaces\n"
				+ " --preserve-quotes          Preserve unneeded quotes\n"
				+ "\n"
				+ "JavaScript Compression Options:\n"
				+ " --disable-optimizations    Disable all micro optimizations\n"
				+ " --line-break <column>      Insert a line break after the specified column number\n"
				+ " --nomunge                  Minify only, do not obfuscate\n"
				+ " --preserve-semi            Preserve all semicolons\n"
				+ "\n"
				+ "XML Compression Options:\n"
				+ " --preserve-comments        Preserve comments\n"
				+ " --preserve-intertag-spaces Preserve intertag spaces\n"
				+ "\n"
				+ "Please note that additional HTML Compressor and YUI Compressor jar\n"
				+ "files must be present in the same directory as this jar file."
				+ "");
	}

}
