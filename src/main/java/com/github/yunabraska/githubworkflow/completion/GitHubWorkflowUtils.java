package com.github.yunabraska.githubworkflow.completion;

import com.github.yunabraska.githubworkflow.config.NodeIcon;
import com.github.yunabraska.githubworkflow.model.GitHubAction;
import com.github.yunabraska.githubworkflow.model.YamlElement;
import com.github.yunabraska.githubworkflow.model.YamlElementHelper;
import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.PrioritizedLookupElement;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.ide.impl.ProjectUtil;
import com.intellij.json.JsonFileType;
import com.intellij.openapi.application.ApplicationInfo;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.vfs.VfsUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.LightVirtualFile;
import com.intellij.util.io.HttpRequests;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.github.api.GithubApiRequest;
import org.jetbrains.plugins.github.api.GithubApiRequestExecutor;
import org.jetbrains.plugins.github.api.GithubApiResponse;
import org.jetbrains.plugins.github.authentication.GHAccountsUtil;
import org.jetbrains.plugins.github.authentication.accounts.GithubAccount;
import org.jetbrains.plugins.github.util.GHCompatibilityUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;

import static com.github.yunabraska.githubworkflow.completion.AutoPopupInsertHandler.addSuffix;
import static com.github.yunabraska.githubworkflow.config.GitHubWorkflowConfig.CACHE_ONE_DAY;
import static com.github.yunabraska.githubworkflow.config.GitHubWorkflowConfig.PATTERN_GITHUB_ENV;
import static com.github.yunabraska.githubworkflow.config.GitHubWorkflowConfig.PATTERN_GITHUB_OUTPUT;
import static com.github.yunabraska.githubworkflow.schema.GitHubActionSchemaProvider.isActionYaml;
import static com.github.yunabraska.githubworkflow.schema.GitHubWorkflowSchemaProvider.isWorkflowYaml;
import static java.util.Optional.ofNullable;

public class GitHubWorkflowUtils {

    public static final Path TMP_DIR = Paths.get(System.getProperty("java.io.tmpdir"), "ide_github_workflow_plugin");
    private static final Logger LOG = Logger.getInstance(GitHubWorkflowUtils.class);

    public static Optional<String[]> getCaretBracketItem(final YamlElement element, final int offset, final String[] prefix) {
        final String wholeText = element.text();
        if (wholeText == null || offset - element.startIndexAbs() < 1) {
            return Optional.empty();
        }
        final int cursorRel = offset - element.startIndexAbs();
        final String offsetText = wholeText.substring(0, cursorRel);
        final int bracketStart = offsetText.lastIndexOf("${{");
        if (cursorRel > 2 && isInBrackets(offsetText, bracketStart) || ofNullable(element.parent()).filter(parent -> "if".equals(parent.key())).isPresent()) {
            return getCaretBracketItem(prefix, wholeText, cursorRel);
        }
        return Optional.empty();
    }

    public static Optional<String[]> getCaretBracketItem(final String[] prefix, final String wholeText, final int cursorRel) {
        final char previousChar = cursorRel == 0 ? ' ' : wholeText.charAt(cursorRel - 1);
        if (cursorRel > 1 && previousChar == '.') {
            //NEXT ELEMENT
            final int indexStart = getStartIndex(wholeText, cursorRel - 1);
            final int indexEnd = getEndIndex(wholeText, cursorRel - 1, wholeText.length());
            return Optional.of(wholeText.substring(indexStart, indexEnd + 1).split("\\."));
        } else if (isNonValidNodeChar(previousChar)) {
            //START ELEMENT
            return Optional.of(prefix);
        } else {
            //MIDDLE ELEMENT
            final int indexStart = cursorRel == 0 ? 0 : getStartIndex(wholeText, cursorRel - 1);
            final String[] prefArray = wholeText.substring(indexStart, cursorRel).split("\\.", -1);
            prefix[0] = prefArray[prefArray.length - 1];
            return Optional.of(wholeText.substring(indexStart, cursorRel - prefix[0].length()).split("\\."));
        }
    }

    private static boolean isNonValidNodeChar(final char c) {
        return !Character.isLetterOrDigit(c) && c != '_' && c != '-';
    }

    public static int getStartIndex(final CharSequence currentText, final int fromIndex) {
        int result = fromIndex;
        while (result > 0) {
            final char c = currentText.charAt(result);
            if (!Character.isLetterOrDigit(c) && c != '_' && c != '-' && c != '.') {
                result = result != fromIndex ? result + 1 : result;
                break;
            }
            result--;
        }
        return Math.min(result, fromIndex);
    }

    public static int getEndIndex(final CharSequence currentText, final int fromIndex, final int toIndex) {
        int result = fromIndex;
        final int endIndex = currentText.length();
        while (result < endIndex && result < toIndex) {
            if (isNonValidNodeChar(currentText.charAt(result))) {
                break;
            }
            result++;
        }
        return result;
    }

    public static String getDefaultPrefix(final CompletionParameters parameters) {
        final String wholeText = parameters.getOriginalFile().getText();
        final int caretOffset = parameters.getOffset();
        final int indexStart = getStartIndex(wholeText, caretOffset - 1);
        return wholeText.substring(indexStart, caretOffset);
    }


    public static boolean isInBrackets(final String partString, final int bracketStart) {
        return bracketStart != -1 && partString.lastIndexOf("}}") <= bracketStart;
    }

    public static String orEmpty(final String text) {
        return ofNullable(text).orElse("");
    }

    public static String getDescription(final YamlElement n) {
        return "r[" + n.required() + "]"
                + ofNullable(n.childDefault()).map(def -> " def[" + def + "]").orElse("")
                + ofNullable(n.description()).map(desc -> " " + desc).orElse("");
    }

    public static Map<String, String> toGithubOutputs(final String text) {
        final Map<String, String> variables = new HashMap<>();
        if (text.contains("$GITHUB_OUTPUT") || text.contains("${GITHUB_OUTPUT}")) {
            final Matcher matcher = PATTERN_GITHUB_OUTPUT.matcher(text);
            while (matcher.find()) {
                if (matcher.groupCount() >= 2) {
                    variables.put(matcher.group(1), matcher.group(2));
                }
            }
        }
        return variables;
    }

    public static Map<String, String> toGithubEnvs(final String text) {
        final Map<String, String> variables = new HashMap<>();
        if (text.contains("GITHUB_ENV") || text.contains("${GITHUB_ENV}")) {
            final Matcher matcher = PATTERN_GITHUB_ENV.matcher(text);
            while (matcher.find()) {
                if (matcher.groupCount() >= 2) {
                    variables.put(matcher.group(1), matcher.group(2));
                }
            }
        }
        return variables;
    }

    public static void addLookupElements(final CompletionResultSet resultSet, final Map<String, String> map, final NodeIcon icon, final char suffix) {
        if (!map.isEmpty()) {
            resultSet.addAllElements(toLookupElements(map, icon, suffix));
        }
    }

    public static List<LookupElement> toLookupElements(final Map<String, String> map, final NodeIcon icon, final char suffix) {
        return map.entrySet().stream().map(item -> toLookupElement(icon, suffix, item.getKey(), item.getValue())).toList();
    }

    public static LookupElement toLookupElement(final NodeIcon icon, final char suffix, final String key, final String text) {
        final LookupElementBuilder result = LookupElementBuilder
                .create(key)
                .withIcon(icon.icon())
                .withBoldness(icon != NodeIcon.ICON_ENV)
                .withTypeText(text)
                .withCaseSensitivity(false)
                .withInsertHandler((ctx, item) -> addSuffix(ctx, item, suffix));
        return PrioritizedLookupElement.withPriority(result, icon.ordinal() + 5d);
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    public static boolean isLineBreak(final char c) {
        return c == '\n' || c == '\r';
    }

    public static VirtualFile downloadSchema(final String url, final String name) {
        try {
            final Path path = TMP_DIR.resolve(name + "_schema.json");
            final VirtualFile newVirtualFile = new LightVirtualFile("github_workflow_plugin_" + path.getFileName().toString(), JsonFileType.INSTANCE, "");
            //FIXME: how to use the intellij idea cache?
            VfsUtil.saveText(newVirtualFile, getOrDownloadContent(url, path, CACHE_ONE_DAY * 30, false));
            return newVirtualFile;
        } catch (final Exception ignored) {
            return null;
        }
    }

    public static String downloadAction(final String url, final GitHubAction gitHubAction) {
        return getOrDownloadContent(url, cachePath(gitHubAction), CACHE_ONE_DAY * 14, true);
    }

    @NotNull
    public static Path cachePath(final GitHubAction gitHubAction) {
        return TMP_DIR.resolve(
                clearString(gitHubAction.actionName())
                        + ofNullable(gitHubAction.slug()).map(GitHubWorkflowUtils::clearString).orElse("")
                        + ofNullable(gitHubAction.sub()).map(GitHubWorkflowUtils::clearString).orElse("")
                        + ofNullable(gitHubAction.ref()).map(GitHubWorkflowUtils::clearString).orElse("")
                        + ofNullable(gitHubAction.actionName()).map(GitHubWorkflowUtils::clearString).orElse("")
                        + "_schema.json"
        );
    }

    private static String clearString(final String input) {
        return input == null ? "" : "_" + input.replace("/", "_").replace("\\", "_");
    }

    public static String downloadFileFromGitHub(final String downloadUrl) {
        return GHAccountsUtil.getAccounts().stream().map(account -> {
            try {
                return downloadFromGitHub(downloadUrl, account);
            } catch (final Exception ignored) {
                return null;
            }
        }).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @SuppressWarnings("DataFlowIssue")
    private static String downloadFromGitHub(final String downloadUrl, final GithubAccount account) throws IOException {
        final String token = GHCompatibilityUtil.getOrRequestToken(account, ProjectUtil.getActiveProject());
        return GithubApiRequestExecutor.Factory.getInstance().create(token).execute(new GithubApiRequest.Get<>(downloadUrl) {
            @SuppressWarnings("BlockingMethodInNonBlockingContext")
            @Override
            public String extractResult(final @NotNull GithubApiResponse response) {
                try {
                    return response.handleBody(inputStream -> {
                        try (final BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                            final StringBuilder stringBuilder = new StringBuilder();
                            String line;
                            while ((line = bufferedReader.readLine()) != null) {
                                stringBuilder.append(line).append(System.lineSeparator());
                            }
                            return stringBuilder.toString();
                        }
                    });
                } catch (final IOException ignored) {
                    return null;
                }
            }
        });
    }

    @SuppressWarnings("BlockingMethodInNonBlockingContext")
    private static String getOrDownloadContent(final String url, final Path path, final long expirationTime, final boolean usingGithub) {
        try {
            final AtomicReference<String> content = new AtomicReference<>(null);
            if (Files.exists(path) && Files.getLastModifiedTime(path).toMillis() > System.currentTimeMillis() - expirationTime) {
                ofNullable(readFileAsync(path, expirationTime)).filter(YamlElementHelper::hasText).ifPresent(content::set);
            }
            if (content.get() != null || downloadContent(url, path, usingGithub, content)) {
                return content.get();
            }
        } catch (final Exception e) {
            LOG.warn("Cache failed for [" + url + "] message [" + (e instanceof NullPointerException ? null : e.getMessage()) + "]");
        }
        return "";
    }

    private static boolean downloadContent(final String url, final Path path, final boolean usingGithub, final AtomicReference<String> content) throws IOException {
        if (!Files.exists(path.getParent())) {
            Files.createDirectories(path.getParent());
        }
        Optional.of(usingGithub)
                .filter(withGH -> withGH)
                .map(withGH -> downloadFileFromGitHub(url))
                .or(() -> ofNullable(downloadContent(url)))
                .filter(YamlElementHelper::hasText)
                .ifPresent(content::set);
        if (content.get() != null) {
            Files.write(path, content.get().getBytes());
            return true;
        }
        return false;
    }

    @SuppressWarnings("BlockingMethodInNonBlockingContext")
    public static String readFileAsync(final Path path, final long expirationTime) {
        LOG.info("Cache load [" + path + "] expires in [" + (System.currentTimeMillis() - expirationTime) + "ms]");
        try (final BufferedReader reader = Files.newBufferedReader(path, Charset.defaultCharset())) {
            final StringBuilder contentBuilder = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append(System.lineSeparator());
            }
            return contentBuilder.toString();
        } catch (final Exception e) {
            try {
                Files.deleteIfExists(path);
            } catch (final Exception ignored) {
                // ignored
            }
            LOG.warn("Failed to read file [" + path + "] message [" + e.getMessage() + "]");
            return null;
        }
    }


    @SuppressWarnings({"java:S2142", "BlockingMethodInNonBlockingContext"})
    private static String downloadContent(final String urlString) {
        LOG.info("Download [" + urlString + "]");
        try {
            final ApplicationInfo applicationInfo = ApplicationInfo.getInstance();
            final Future<String> future = ApplicationManager.getApplication().executeOnPooledThread(() -> {
                try {
                    return HttpRequests
                            .request(urlString)
                            .gzip(true)
                            .readTimeout(5000)
                            .connectTimeout(5000)
                            .userAgent(applicationInfo.getBuild().getProductCode() + "/" + applicationInfo.getFullVersion())
                            .tuner(request -> request.setRequestProperty("Client-Name", "GitHub Workflow Plugin"))
                            .readString();
                } catch (final Exception e) {
                    return null;
                }
            });
            return future.get();
        } catch (final Exception e) {
            LOG.warn("Execution failed for [" + urlString + "] message [" + (e instanceof NullPointerException ? null : e.getMessage()) + "]");
        }
        return "";
    }

    public static Optional<Path> getWorkflowFile(final PsiElement psiElement) {
        return Optional.ofNullable(psiElement)
                .map(PsiElement::getContainingFile)
                .map(PsiFile::getOriginalFile)
                .map(PsiFile::getViewProvider)
                .map(FileViewProvider::getVirtualFile)
                .map(VirtualFile::getPath)
                .map(Paths::get)
                .filter(GitHubWorkflowUtils::isWorkflowPath);
    }

    public static boolean isWorkflowPath(final Path path) {
        return path != null && (isActionYaml(path) || isWorkflowYaml(path));
    }

    public static boolean isYamlFile(final Path path) {
        return path.getName(path.getNameCount() - 1).toString().toLowerCase().endsWith(".yml") || path.getName(path.getNameCount() - 1).toString().toLowerCase().endsWith(".yaml");
    }

    private GitHubWorkflowUtils() {

    }
}
