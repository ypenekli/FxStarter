package fxlauncher;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Map;
import java.util.stream.Collectors;

import javax.xml.bind.JAXB;

public class CreateManifest {
	private static ArrayList<String> includeExtensions = new ArrayList<>();

	static {
		includeExtensions.addAll(Arrays.asList("jar", "war", "pdf"));
	}

	private static final String uri = "--uri";
	private static final String LAUNCH = "--launch";
	private static final String DEPLOY_DIR = "--deployDir";
	private static final String SILENT = "--silent";
	private static final String CACHE_DIR = "cacheDir";
	private static final String ACCEPT_DOWNGRADE = "acceptDowngrade";
	private static final String WHATS_NEW_PAGE = "--whatsNewPage";
	private static final String PRE_LOAT_NATIVE_LIBS = "--preloadNativeLibraries";
	private static final String INCLUDE_EXTENSIONS = "--include-extensions";

	private static final String FILE_NAME = "--filename";
	private static final String UPDATE_TEXT = "--updateText";
	private static final String UPDATE_LABEL_STYLE = "--updateLabelStyle";
	private static final String PROGRESS_BAR_STYLE = "--progressBarStyle";
	private static final String WRAPPER_STYLE = "--wrapperStyle";
	private static final String APP_XML = "app.xml";
	private static final String SILENT_FILE = "app.txt";

	public static void main(String[] args) throws IOException {
		Map<String, String> params = initParams(args);

		if (params != null && params.containsKey(uri) && params.containsKey(LAUNCH) && params.containsKey(DEPLOY_DIR)) {
			URI baseURI = URI.create(params.get(uri));
			String launchClass = params.get(LAUNCH);
			Path appPath = Paths.get(params.get(DEPLOY_DIR));
			FXManifest manifest = create(baseURI, launchClass, appPath);
			update(params, manifest);
			JAXB.marshal(manifest, appPath.resolve(APP_XML).toFile());
			System.out.println(String.format("Manifest file is createted at location : %s\\%s", appPath, APP_XML));
		}
	}

	private static void update(Map<String, String> params, FXManifest manifest) {
		if (params.containsKey(CACHE_DIR))
			manifest.cacheDir = params.get(CACHE_DIR);
		if (params.containsKey(WHATS_NEW_PAGE))
			manifest.whatsNewPage = params.get(WHATS_NEW_PAGE);
		if (params.containsKey(UPDATE_TEXT))
			manifest.updateText = params.get(UPDATE_TEXT);
		if (params.containsKey(UPDATE_LABEL_STYLE))
			manifest.updateLabelStyle = params.get(UPDATE_LABEL_STYLE);
		if (params.containsKey(PROGRESS_BAR_STYLE))
			manifest.progressBarStyle = params.get(PROGRESS_BAR_STYLE);
		if (params.containsKey(WRAPPER_STYLE))
			manifest.wrapperStyle = params.get(WRAPPER_STYLE);
		if (params.containsKey(FILE_NAME))
			manifest.parameters = String.format("%s=%s", FILE_NAME, params.get(FILE_NAME));
		if (params.containsKey(ACCEPT_DOWNGRADE))
			manifest.acceptDowngrade = Boolean.valueOf(params.get(ACCEPT_DOWNGRADE));
		if (params.containsKey(PRE_LOAT_NATIVE_LIBS))
			manifest.preloadNativeLibraries = params.get(PRE_LOAT_NATIVE_LIBS);
		if (params.containsKey(INCLUDE_EXTENSIONS)) {
			includeExtensions.addAll(Arrays.stream(params.get(INCLUDE_EXTENSIONS).split(","))
					.filter(s -> s != null && !s.isEmpty()).collect(Collectors.toList()));
		}
	}

	public static FXManifest create(URI baseURI, String launchClass, Path appPath) throws IOException {
		FXManifest manifest = new FXManifest();
		manifest.ts = System.currentTimeMillis();
		manifest.uri = baseURI;
		manifest.launchClass = launchClass;

		Files.walkFileTree(appPath, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (!Files.isDirectory(file) && shouldIncludeInManifest(file)
						&& !file.getFileName().toString().startsWith("FxStarter"))
					manifest.files.add(new LibraryFile(appPath, file));
				return FileVisitResult.CONTINUE;
			}
		});

		return manifest;
	}

	private static boolean shouldIncludeInManifest(Path file) {
		String filename = file.getFileName().toString();
		for (String ext : includeExtensions) {
			if (filename.toLowerCase().endsWith(String.format(".%s", ext.toLowerCase())))
				return true;
		}
		return false;
	}

	private static final String EQUAL = "=";
	private static final String COLON_ESCAPE = "':'";
	private static final String COLON = ":";
	private static final String SEMI_COLON_ESCAPE = "';'";
	private static final String SEMI_COLON = ";";

	private static Map<String, String> initParams(final String[] args) {
		Map<String, String> params = null;
		if (args != null && args.length > 0) {
			params = new Hashtable<>(args.length);
			for (final String line : args) {
				splitKeyValue(params, line);
			}
			if (params.containsKey(SILENT)) {
				Path file = Paths.get(params.get(DEPLOY_DIR), SILENT_FILE);
				try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
					params = new Hashtable<>(10);
					params.put(DEPLOY_DIR, file.getParent().toString());
					String line;
					while ((line = br.readLine()) != null) {
						splitKeyValue(params, line);
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return params;
	}

	private static void splitKeyValue(Map<String, String> params, String line) {
		int i = 0;
		if ((i = line.indexOf(EQUAL)) > 0)
			params.put(line.substring(0, i),
					line.substring(i + 1).replace(COLON_ESCAPE, COLON).replace(SEMI_COLON_ESCAPE, SEMI_COLON));
	}

//	private static Map<String, String> readParams(final String[] args) throws IOException {
//		Map<String, String> params = null;
//		if (args != null && args.length > 0) {
//			Path file = Paths.get(args[1], SILENT_FILE);
//			try (BufferedReader br = new BufferedReader(new FileReader(file.toFile()))) {
//				params = new Hashtable<>(10);
//				params.put(APP_DIR, file.getParent().toString());
//				String line;
//				while ((line = br.readLine()) != null) {
//					int i = line.indexOf(EQUAL);
//					if (i > 0) {
//						params.put(line.substring(0, i), line.substring(i + 1));
//					}
//				}
//			}
//		}
//		return params;
//	}

}
