package com.github.yunabraska.githubworkflow.highlights;

import com.github.yunabraska.githubworkflow.model.CompletionItem;
import com.github.yunabraska.githubworkflow.model.WorkflowContext;
import com.github.yunabraska.githubworkflow.model.YamlElement;
import com.github.yunabraska.githubworkflow.quickfixes.QuickFix;
import com.github.yunabraska.githubworkflow.quickfixes.ReplaceTextIntentionAction;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.icons.AllIcons;
import com.intellij.lang.annotation.AnnotationBuilder;
import com.intellij.lang.annotation.AnnotationHolder;
import com.intellij.lang.annotation.Annotator;
import com.intellij.lang.annotation.HighlightSeverity;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.impl.source.tree.LeafElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.yaml.YAMLLanguage;
import org.jetbrains.yaml.psi.YAMLKeyValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.github.yunabraska.githubworkflow.config.GitHubWorkflowConfig.*;
import static com.github.yunabraska.githubworkflow.model.CompletionItem.listEnvs;
import static com.github.yunabraska.githubworkflow.model.CompletionItem.listInputs;
import static com.github.yunabraska.githubworkflow.model.CompletionItem.listJobOutputs;
import static com.github.yunabraska.githubworkflow.model.CompletionItem.listJobs;
import static com.github.yunabraska.githubworkflow.model.CompletionItem.listSecrets;
import static com.github.yunabraska.githubworkflow.model.CompletionItem.listStepOutputs;
import static com.github.yunabraska.githubworkflow.model.CompletionItem.listSteps;
import static com.github.yunabraska.githubworkflow.model.PsiElementProcessor.processPsiElement;
import static com.github.yunabraska.githubworkflow.model.WorkflowContext.WORKFLOW_CONTEXT_MAP;
import static com.github.yunabraska.githubworkflow.model.YamlElementHelper.getPath;
import static com.github.yunabraska.githubworkflow.model.YamlElementHelper.hasText;
import static com.intellij.lang.annotation.HighlightSeverity.INFORMATION;
import static java.util.Optional.ofNullable;

public class HighlightAnnotator implements Annotator {

    public static final Pattern CARET_BRACKET_ITEM_PATTERN = Pattern.compile("[\\s|\\t^{]\\b(\\w++(?:\\.\\w++)++)[\\s|\\t$}]");
    //    public static final Pattern CARET_BRACKET_ITEM_PATTERN = Pattern.compile("\\b(\\w++(?:\\.\\w++)++)\\b");
    private static final Key<Boolean> ANNOTATED_KEY = new Key<>(HighlightAnnotator.class.getSimpleName());

    @Override
    public void annotate(@NotNull final PsiElement psiElement, @NotNull final AnnotationHolder holder) {
        if (psiElement.isValid()) {
            processPsiElement(holder, psiElement);
//            ofNullable(psiElement.getUserData(KEY_QUICK_FIX)).ifPresent(fix -> fix.forEach(f -> f.createAnnotation(psiElement, holder)));
        } else {
            final Project project = psiElement.getProject();
            if (psiElement.getLanguage() instanceof YAMLLanguage) {
                ofNullable(WORKFLOW_CONTEXT_MAP.get(getPath(psiElement))).map(WorkflowContext::root).map(root -> toYamlElement(psiElement, root)).ifPresent(element -> {
//                    if (FIELD_USES.equals(element.key())) {
//                        ofNullable(element.childTextNoQuotes()).map(GitHubAction::getGitHubAction).filter(GitHubAction::isAvailable).ifPresent(gitHubAction -> {
//                            final String browserText = "Open in Browser [" + gitHubAction.slug() + "]";
//                            final String marketplaceText = "Open in Marketplace [" + gitHubAction.slug() + "]";
//                            final List<QuickFix> quickFixes = gitHubAction.isAction()
//                                    ? Arrays.asList(new OpenUrlIntentionAction(gitHubAction.marketplaceUrl(), marketplaceText, null), new OpenUrlIntentionAction(gitHubAction.toUrl(), browserText, null))
//                                    : List.of(new OpenUrlIntentionAction(gitHubAction.toUrl(), browserText, null));
//                            create(
//                                    psiElement,
//                                    holder,
//                                    INFORMATION,
//                                    ProblemHighlightType.INFORMATION,
//                                    quickFixes,
//                                    psiElement.getTextRange(),
//                                    browserText
//                            );
//                        });
//                    }
                    //VALIDATE ACTION INPUTS
//                    if (!(psiElement instanceof LeafElement) && element.key() != null && ofNullable(element.parent()).map(YamlElement::key).filter(FIELD_WITH::equals).isPresent()) {
//                        element.findParentStep().map(YamlElement::uses).map(GitHubAction::getGitHubAction).map(action -> action.inputsA(() -> psiElement)).map(Map::keySet).ifPresent(inputs -> {
//                            if (!inputs.contains(element.key())) {
//                                create(
//                                        psiElement,
//                                        holder,
//                                        HighlightSeverity.ERROR,
//                                        ProblemHighlightType.GENERIC_ERROR,
//                                        List.of(new ReplaceTextIntentionAction(psiElement.getTextRange(), element.key(), true, null)),
//                                        psiElement.getTextRange(),
//                                        "Invalid [" + element.key() + "]"
//                                );
//                            }
//                        });
//                    }
//                    if (!(psiElement instanceof LeafElement) && element.findParent(FIELD_USES).isPresent()) {
//                        ofNullable(ACTION_CACHE.get(element.textOrChildText())).ifPresent(action -> {
//                            if (action.isLocal() && action.isAvailable()) {
//                                create(
//                                        psiElement,
//                                        holder,
//                                        INFORMATION,
//                                        ProblemHighlightType.INFORMATION,
//                                        List.of(new JumpToFile(action, AllIcons.Gutter.ImplementedMethod)),
//                                        element.textRange(),
//                                        "Jump to file [" + ofNullable(action.slug()).orElseGet(action::uses) + "]"
//                                );
//                            } else if (!action.isLocal()) {
//                                create(
//                                        psiElement,
//                                        holder,
//                                        action.isAvailable() ? INFORMATION : HighlightSeverity.WEAK_WARNING,
//                                        action.isAvailable() ? ProblemHighlightType.INFORMATION : ProblemHighlightType.WEAK_WARNING,
//                                        action.isAvailable() ? List.of(new ReloadGhaAction(action, AllIcons.General.InlineRefresh)) : List.of(new ReloadGhaAction(action, AllIcons.General.InlineRefresh), new OpenSettingsIntentionAction(p -> action.deleteCache(), AllIcons.General.Settings)),
//                                        element.textRange(),
//                                        action.isAvailable() ? "Reload [" + ofNullable(action.slug()).orElseGet(action::uses) + "]" : "Unresolved [" + ofNullable(action.slug()).orElseGet(action::uses) + "]"
//                                );
//                            }
//                        });
//                    } else
                    if (element.parent() != null && (FIELD_RUN.equals(element.parent().key())
                            || "if".equals(element.parent().key())
                            || "name".equals(element.parent().key())
                            || ("value".equals(element.parent().key()) && element.findParentOutput().isPresent())
                            || (element.parent() != null && element.parent().parent() != null && FIELD_WITH.equals(element.parent().parent().key()))
                            || (element.parent() != null && element.parent().parent() != null && FIELD_ENVS.equals(element.parent().parent().key()))
                            || (element.parent() != null && element.parent().parent() != null && FIELD_OUTPUTS.equals(element.parent().parent().key()))
                    )) {
                        //TODO: Find solution for undetected items with only one '.' e.g. [inputs.]
                        //  MAYBE: regex needs to have '${{ }}', only 'if' content is different
                        processBracketItems(project, psiElement, holder, element);
//                    } else if (FIELD_NEEDS.equals(element.key())) {
//                        element.findParentJob().ifPresent(job -> {
//                            final List<String> jobs = element.context().jobs().values().stream().filter(j -> j.startIndexAbs() < job.startIndexAbs()).map(YamlElement::key).toList();
//                            element.children().forEach(jobChild -> {
//                                final String jobId = jobChild.textOrChildTextNoQuotes().trim();
//                                final TextRange range = new TextRange(jobChild.startIndexAbs(), jobChild.startIndexAbs() + jobId.length());
//                                if (!jobs.contains(jobId)) {
//                                    //INVALID JOB_ID
//                                    create(
//                                            psiElement,
//                                            holder,
//                                            HighlightSeverity.ERROR,
//                                            ProblemHighlightType.GENERIC_ERROR,
//                                            jobs.stream().map(need -> new ReplaceTextIntentionAction(range, need, false, null)).map(ia -> (QuickFix) ia).toList(),
//                                            range,
//                                            "Invalid [" + jobId + "] - needs to be a valid jobId from previous jobs"
//                                    );
//                                } else {
//                                    //UNUSED JOB_ID
//                                    if (job.allElements().noneMatch(child -> child.text() != null && child.text().contains(FIELD_NEEDS + "." + jobId + "."))) {
//                                        create(
//                                                psiElement,
//                                                holder,
//                                                INFORMATION,
//                                                ProblemHighlightType.INFORMATION,
//                                                List.of(new ReplaceTextIntentionAction(range, jobId, true, null)),
//                                                range,
//                                                "Unused [" + jobId + "]"
//                                        );
//                                    }
//                                }
//                            });
//                        });
                    } else if (FIELD_OUTPUTS.equals(element.key())) {
                        //CHECK FOR UNUSED JOB OUTPUTS
                        element.findParentJob().map(YamlElement::key).ifPresent(jobId -> {
                            final List<String> usedOutputs = element.context().outputs().values().stream()
                                    .filter(output -> output.findParentOn().isPresent())
                                    .map(output -> output.child("value").orElse(null))
                                    .filter(Objects::nonNull)
                                    .map(YamlElement::textOrChildText)
                                    .flatMap(value -> {
                                        final List<String[]> result = new ArrayList<>();
                                        final Matcher matcher = CARET_BRACKET_ITEM_PATTERN.matcher(value);
                                        while (matcher.find()) {
                                            result.add(matcher.group().split("\\."));
                                        }
                                        return result.stream();
                                    })
                                    .filter(parts -> parts.length == 4)
                                    .filter(parts -> FIELD_JOBS.equals(parts[0]))
                                    .filter(parts -> jobId.equals(parts[1]))
                                    .filter(parts -> FIELD_OUTPUTS.equals(parts[2]))
                                    .map(parts -> parts[3])
                                    .toList();
                            element.children().stream().filter(output -> output.key() != null).filter(output -> !usedOutputs.contains(output.key())).forEach(unusedOutput -> {
                                final TextRange range = new TextRange(unusedOutput.startIndexAbs(), unusedOutput.children().stream().mapToInt(YamlElement::endIndexAbs).max().orElseGet(unusedOutput::endIndexAbs));
                                create(
                                        psiElement,
                                        holder,
                                        HighlightSeverity.WEAK_WARNING,
                                        ProblemHighlightType.LIKE_UNUSED_SYMBOL,
                                        List.of(new ReplaceTextIntentionAction(range, unusedOutput.key(), true, null)),
                                        range,
                                        "Unused [" + unusedOutput.key() + "]"
                                );
                            });
                        });
                    }
                    // SHOW Output Env && Output Variable declaration
                    if (psiElement instanceof LeafElement && element.parent() != null && FIELD_RUN.equals(element.parent().key()) && hasText(psiElement.getText()) && (PATTERN_GITHUB_OUTPUT.matcher(psiElement.getText()).find() || PATTERN_GITHUB_ENV.matcher(psiElement.getText()).find())) {
                        holder.newSilentAnnotation(INFORMATION).gutterIconRenderer(new IconRenderer(null, psiElement, AllIcons.Nodes.Gvariable)).create();
                    }
                });
            }
        }
    }

    private static void processBracketItems(final Project project, @NotNull final PsiElement psiElement, @NotNull final AnnotationHolder holder, final YamlElement element) {
        final Matcher matcher = CARET_BRACKET_ITEM_PATTERN.matcher(psiElement.getText());
        while (matcher.find()) {
            final String[] parts = Arrays.stream(matcher.group().split("\\."))
                    .map(s -> s.replace("{", ""))
                    .map(s -> s.replace("}", ""))
                    .map(String::trim)
                    .toArray(String[]::new);
            final String scope = parts[0];
            switch (scope) {
                case FIELD_INPUTS ->
                        ifEnoughItems(holder, psiElement, parts, 2, 2, inputId -> isDefinedItem0(psiElement, holder, matcher, inputId, listInputs(element).stream().map(CompletionItem::key).toList()));
                case FIELD_SECRETS -> ifEnoughItems(holder, psiElement, parts, 2, 2, secretId -> {
                    final List<String> secrets = listSecrets(element).stream().map(CompletionItem::key).toList();
                    if (!secrets.contains(secretId)) {
                        final TextRange textRange = simpleTextRange(psiElement, matcher, secretId);
                        create(
                                psiElement,
                                holder,
                                HighlightSeverity.WEAK_WARNING,
                                ProblemHighlightType.WEAK_WARNING,
                                secrets.stream().map(output -> new ReplaceTextIntentionAction(textRange, output, false, null)).map(ia -> (QuickFix) ia).toList(),
                                textRange,
                                "Undefined [" + secretId + "] - it might be provided at runtime"
                        );
                    }
                });
                case FIELD_ENVS ->
                        ifEnoughItems(holder, psiElement, parts, 2, -1, envId -> isDefinedItem0(psiElement, holder, matcher, envId, listEnvs(element, element.startIndexAbs()).stream().map(CompletionItem::key).toList()));
                case FIELD_GITHUB ->
                        ifEnoughItems(holder, psiElement, parts, 2, -1, envId -> isDefinedItem0(psiElement, holder, matcher, envId, new ArrayList<>(DEFAULT_VALUE_MAP.get(FIELD_GITHUB).get().keySet())));
                case FIELD_RUNNER ->
                        ifEnoughItems(holder, psiElement, parts, 2, 2, runnerId -> isDefinedItem0(psiElement, holder, matcher, runnerId, new ArrayList<>(DEFAULT_VALUE_MAP.get(FIELD_RUNNER).get().keySet())));
                case FIELD_STEPS -> ifEnoughItems(holder, psiElement, parts, 4, 4, stepId -> {
                    final List<String> steps = listSteps(element).stream().map(CompletionItem::key).toList();
                    if (isDefinedItem0(psiElement, holder, matcher, stepId, steps) && isField2Valid(psiElement, holder, matcher, parts[2])) {
                        final List<String> outputs = listStepOutputs(project, element, element.startIndexAbs(), stepId).stream().map(CompletionItem::key).toList();
                        isValidItem3(psiElement, holder, matcher, parts[3], outputs);

                    }
                });
                case FIELD_JOBS ->
                    // TODO: CHECK OUTPUTS FOR JOBS && NEEDS && STEPS e.g. [ if (!FIELD_OUTPUTS.equals(parts[2])) ]
                        ifEnoughItems(holder, psiElement, parts, 4, 4, jobId -> {
                            final List<String> jobs = listJobs(element).stream().map(CompletionItem::key).toList();
                            //noinspection DuplicatedCode
                            if (isDefinedItem0(psiElement, holder, matcher, jobId, jobs) && isField2Valid(psiElement, holder, matcher, parts[2])) {
                                final List<String> outputs = listJobOutputs(project, element, jobId).stream().map(CompletionItem::key).toList();
                                isValidItem3(psiElement, holder, matcher, parts[3], outputs);
                            }
                        });
                case FIELD_NEEDS ->
                        ifEnoughItems(holder, psiElement, parts, 4, 4, jobId -> element.findParentJob().flatMap(job -> job.child(FIELD_NEEDS)).ifPresent(needElement -> {
                            final Set<String> needs = needElement.needItems();
                            //noinspection DuplicatedCode
                            if (isDefinedItem0(psiElement, holder, matcher, jobId, needs) && isField2Valid(psiElement, holder, matcher, parts[2])) {
                                final List<String> outputs = listJobOutputs(project, element, jobId).stream().map(CompletionItem::key).toList();
                                isValidItem3(psiElement, holder, matcher, parts[3], outputs);
                            }
                        }));
                default -> {
                    // ignored
                }
            }
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean isField2Valid(@NotNull final PsiElement psiElement, @NotNull final AnnotationHolder holder, final Matcher matcher, final String itemId) {
        if (!FIELD_OUTPUTS.equals(itemId)) {
            final TextRange textRange = simpleTextRange(psiElement, matcher, itemId);
            create(
                    psiElement,
                    holder,
                    HighlightSeverity.ERROR,
                    ProblemHighlightType.GENERIC_ERROR,
                    List.of(new ReplaceTextIntentionAction(textRange, FIELD_OUTPUTS, false, null)),
                    textRange,
                    "Invalid [" + itemId + "]"
            );
            return false;
        }
        return true;
    }

    private static void isValidItem3(@NotNull final PsiElement psiElement, @NotNull final AnnotationHolder holder, final Matcher matcher, final String itemId, final List<String> outputs) {
        if (!outputs.contains(itemId)) {
            final TextRange textRange = simpleTextRange(psiElement, matcher, itemId);
            create(
                    psiElement,
                    holder,
                    HighlightSeverity.ERROR,
                    ProblemHighlightType.GENERIC_ERROR,
                    outputs.stream().map(output -> new ReplaceTextIntentionAction(textRange, output, false, null)).map(ia -> (QuickFix) ia).toList(),
                    textRange,
                    "Undefined [" + itemId + "]"
            );
        }
    }

    private static boolean isDefinedItem0(@NotNull final PsiElement psiElement, @NotNull final AnnotationHolder holder, final Matcher matcher, final String itemId, final Collection<String> items) {
        if (!items.contains(itemId)) {
            final TextRange textRange = simpleTextRange(psiElement, matcher, itemId);
            create(
                    psiElement,
                    holder,
                    HighlightSeverity.ERROR,
                    ProblemHighlightType.GENERIC_ERROR,
                    items.stream().map(output -> new ReplaceTextIntentionAction(textRange, output, false, null)).map(ia -> (QuickFix) ia).toList(),
                    textRange,
                    "Undefined [" + itemId + "]"
            );
            return false;
        }
        return true;
    }

    private static YamlElement toYamlElement(final PsiElement psiElement, final YamlElement root) {
        return psiElement instanceof final YAMLKeyValue kvPSI && kvPSI.getKey() != null && kvPSI.getKey().getTextRange() != null
                ? root.allElements()
                .filter(element -> element.startIndexAbs() > -1 && element.startIndexAbs() <= kvPSI.getKey().getTextRange().getStartOffset())
                .filter(element -> element.endIndexAbs() > -1 && element.endIndexAbs() >= kvPSI.getKey().getTextRange().getEndOffset())
                .filter(element -> element.textNoQuotes() != null && !element.textNoQuotes().isEmpty() && !element.textNoQuotes().isBlank())
                .findFirst()
                .orElseGet(() -> root.context().getClosestElement(psiElement.getTextOffset()).orElse(null))
                : root.allElements()
                .filter(element -> element.startIndexAbs() > -1 && element.startIndexAbs() <= psiElement.getTextRange().getStartOffset())
                .filter(element -> element.endIndexAbs() > -1 && element.endIndexAbs() >= psiElement.getTextRange().getEndOffset())
                .filter(element -> element.textNoQuotes() != null && !element.textNoQuotes().isEmpty() && !element.textNoQuotes().isBlank())
                .findFirst()
                .orElseGet(() -> root.context().getClosestElement(psiElement.getTextOffset()).orElse(null));
    }

    private static TextRange simpleTextRange(@NotNull final PsiElement psiElement, final Matcher matcher, final String itemId) {
        final int start = psiElement.getTextRange().getStartOffset() + psiElement.getText().indexOf(itemId, matcher.start(0));
        return new TextRange(start, start + itemId.length());
    }

    private static TextRange fixRange(final PsiElement psiElement, final TextRange range) {
        if (range != null) {
            final int newStart = Math.max(psiElement.getTextRange().getStartOffset(), range.getStartOffset());
            final int newEnd = Math.min(psiElement.getTextRange().getEndOffset(), range.getEndOffset());
            return newStart >= newEnd ? null : new TextRange(newStart, newEnd);
        }
        return null;
    }

    private static void ifEnoughItems(
            final AnnotationHolder holder,
            final PsiElement psiElement,
            final String[] parts,
            final int min,
            final int max,
            final Consumer<String> then
    ) {
        if (parts.length < min || parts.length < 2) {
            final String unfinishedStatement = String.join(".", parts);
            final int startOffset = psiElement.getTextRange().getStartOffset() + psiElement.getText().indexOf(unfinishedStatement);
            final TextRange textRange = new TextRange(startOffset, startOffset + unfinishedStatement.length());
            create(
                    psiElement,
                    holder,
                    HighlightSeverity.ERROR,
                    ProblemHighlightType.GENERIC_ERROR,
                    null,
                    textRange,
                    "Incomplete statement [" + unfinishedStatement + "]"
            );
        } else if (max != -1 && parts.length > max) {
            final String fullStatement = String.join(".", parts);
            final String longPart = "." + String.join(".", (Arrays.copyOfRange(parts, max, parts.length)));
            final int statementStartIndex = psiElement.getText().indexOf(fullStatement);
            final int startOffset = psiElement.getTextRange().getStartOffset() + statementStartIndex + fullStatement.lastIndexOf(longPart);
            final TextRange textRange = new TextRange(startOffset, startOffset + longPart.length());
            create(
                    psiElement,
                    holder,
                    HighlightSeverity.ERROR,
                    ProblemHighlightType.GENERIC_ERROR,
                    List.of(new ReplaceTextIntentionAction(textRange, longPart, true, null)),
                    textRange,
                    "Not valid here [" + longPart + "]"
            );
        } else {
            then.accept(parts[1]);
        }
    }

    public static void create(
            final PsiElement psiElement,
            final AnnotationHolder holder,
            final HighlightSeverity level,
            final ProblemHighlightType type,
            final Collection<QuickFix> quickFixes,
            final TextRange range,
            final String message
    ) {
        final TextRange textRange = fixRange(psiElement, range);
        if (textRange != null) {
            final List<QuickFix> safeQuickFixes = quickFixes != null ? new ArrayList<>(quickFixes) : new ArrayList<>();
            if (safeQuickFixes.isEmpty()) {
                safeQuickFixes.add(null);
            }
            for (final QuickFix fix : safeQuickFixes) {
                createAnnotation(
                        psiElement,
                        holder,
                        level,
                        type,
                        fix,
                        textRange,
                        message
                );
            }
        }
    }

    @SuppressWarnings({"DataFlowIssue", "ResultOfMethodCallIgnored"})
    private static void createAnnotation(
            final PsiElement psiElement,
            final AnnotationHolder holder,
            final HighlightSeverity level,
            final ProblemHighlightType type,
            final QuickFix quickFix,
            final TextRange range,
            final String message
    ) {
        final AnnotationBuilder annotation = holder.newAnnotation(level, message);
        final AnnotationBuilder silentAnnotation = holder.newSilentAnnotation(level);
        ofNullable(range).ifPresent(annotation::range);
        ofNullable(type).ifPresent(annotation::highlightType);
        ofNullable(message).ifPresent(annotation::tooltip);
        ofNullable(quickFix).ifPresent(fix -> {
            annotation.withFix(fix);
            ofNullable(fix.icon()).map(icon -> new IconRenderer(fix, psiElement, icon)).ifPresent(annotation::gutterIconRenderer);
        });

        ofNullable(range).ifPresent(silentAnnotation::range);
        ofNullable(quickFix).ifPresent(silentAnnotation::withFix);

        annotation.create();
        silentAnnotation.create();
    }
}
