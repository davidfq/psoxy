package co.worklytics.psoxy.rules.github;

import co.worklytics.psoxy.rules.RESTRules;
import co.worklytics.psoxy.rules.Rules2;
import com.avaulta.gateway.rules.Endpoint;
import com.avaulta.gateway.rules.JsonSchemaFilterUtils;
import com.avaulta.gateway.rules.transforms.Transform;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
public class PrebuiltSanitizerRules {

    private static final List<String> commonAllowedQueryParameters = Lists.newArrayList(
            "per_page",
            "page"
    );

    private static final List<String> commonDirectionAllowedQueryParameters = Lists.newArrayList(
            "sort",
            "direction"
    );

    private static final List<String> orgMembersAllowedQueryParameters = Streams.concat(commonAllowedQueryParameters.stream(),
                    Lists.newArrayList("filter",
                            "role").stream())
            .collect(Collectors.toList());

    private static final List<String> orgAuditLogAllowedQueryParameters = Streams.concat(commonAllowedQueryParameters.stream(),
                    Lists.newArrayList("phrase",
                            "include",
                            "after",
                            "before",
                            "order").stream())
            .collect(Collectors.toList());

    private static final List<String> issuesAllowedQueryParameters = Streams.concat(commonAllowedQueryParameters.stream(),
                    commonDirectionAllowedQueryParameters.stream(),
                    Lists.newArrayList("milestone",
                            "state",
                            "labels",
                            "since").stream())
            .collect(Collectors.toList());

    private static final List<String> issueCommentsQueryParameters = Streams.concat(commonAllowedQueryParameters.stream(),
                    Lists.newArrayList("since").stream())
            .collect(Collectors.toList());

    private static final List<String> repoListAllowedQueryParameters = Streams.concat(commonAllowedQueryParameters.stream(),
                    commonDirectionAllowedQueryParameters.stream(),
                    Lists.newArrayList("type").stream())
            .collect(Collectors.toList());

    private static final List<String> repoBranchesAllowedQueryParameters = Streams.concat(commonAllowedQueryParameters.stream(),
                    Lists.newArrayList("protected").stream())
            .collect(Collectors.toList());

    private static final List<String> pullsAllowedQueryParameters = Streams.concat(commonAllowedQueryParameters.stream(),
                    commonDirectionAllowedQueryParameters.stream(),
                    Lists.newArrayList("state",
                            "head",
                            "base").stream())
            .collect(Collectors.toList());

    private static final List<String> pullsCommentsQueryParameters = Streams.concat(commonAllowedQueryParameters.stream(),
                    commonDirectionAllowedQueryParameters.stream(),
                    Lists.newArrayList("since").stream())
            .collect(Collectors.toList());

    private static final List<String> commitsAllowedQueryParameters = Streams.concat(commonAllowedQueryParameters.stream(),
                    Lists.newArrayList("sha",
                            "path",
                            "since",
                            "until").stream())
            .collect(Collectors.toList());

    static final Endpoint ORG_MEMBERS = Endpoint.builder()
            .pathTemplate("/orgs/{org}/members")
            .allowedQueryParams(orgMembersAllowedQueryParameters)
            .transforms(generateUserTransformations("."))
            .build();

    static final Endpoint GRAPHQL_FOR_USERS = Endpoint.builder()
            .pathTemplate("/graphql")
            .transform(Transform.Redact.builder()
                    .jsonPath("$..ssoUrl")
                    .build())
            .transform(Transform.Pseudonymize.builder()
                    .jsonPath("$..nameId")
                    .jsonPath("$..login")
                    .jsonPath("$..email")
                    .jsonPath("$..guid")
                    .build())
            .responseSchema(jsonSchemaForUserQueryResult())
            .build();

    static final Endpoint ORG_TEAMS = Endpoint.builder()
            .pathTemplate("/orgs/{org}/teams")
            .allowedQueryParams(commonAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..name")
                    .jsonPath("$..description")
                    .build())
            .build();

    static final Endpoint ORG_AUDIT_LOG = Endpoint.builder()
            .pathTemplate("/orgs/{org}/audit-log")
            .allowedQueryParams(orgAuditLogAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..hashed_token")
                    .jsonPath("$..business")
                    .jsonPath("$..business_id")
                    .jsonPath("$..transport_protocol")
                    .jsonPath("$..transport_protocol_name")
                    .jsonPath("$..pull_request_title")
                    .jsonPath("$..user_agent")
                    .build())
            .transform(Transform.Pseudonymize.builder()
                    .jsonPath("$..actor")
                    .jsonPath("$..user")
                    .jsonPath("$..userId")
                    .build())
            .build();

    static final Endpoint ORG_TEAM_MEMBERS = Endpoint.builder()
            .pathTemplate("/orgs/{org}/teams/{team_slug}/members")
            .allowedQueryParams(commonAllowedQueryParameters)
            .transforms(generateUserTransformations("."))
            .build();

    static final Endpoint REPO_COMMIT = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/commits/{ref}")
            .allowedQueryParams(commonAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..name")
                    .jsonPath("$..message")
                    .jsonPath("$..files")
                    .jsonPath("$..signature")
                    .jsonPath("$..payload")
                    .build())
            .transform(Transform.Pseudonymize.builder()
                    .jsonPath("$.commit..email")
                    .build())
            .transforms(generateUserTransformations("..author"))
            .transforms(generateUserTransformations("..committer"))
            .build();

    static final Endpoint ISSUES = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/issues")
            .allowedQueryParams(issuesAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..title")
                    .jsonPath("$..body")
                    .jsonPath("$..description")
                    .jsonPath("$..name")
                    .build())
            .transforms(generateUserTransformations("..user"))
            .transforms(generateUserTransformations("..assignee"))
            .transforms(generateUserTransformations("..assignees[*]"))
            .transforms(generateUserTransformations("..creator"))
            .transforms(generateUserTransformations("..closed_by"))
            .build();

    static final Endpoint ISSUE = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/issues/{issue_number}")
            .transform(Transform.Redact.builder()
                    .jsonPath("$..title")
                    .jsonPath("$..body")
                    .jsonPath("$..description")
                    .jsonPath("$..name")
                    .jsonPath("$..pem")
                    .build())
            .transforms(generateUserTransformations("..user"))
            .transforms(generateUserTransformations("..assignee"))
            .transforms(generateUserTransformations("..assignees[*]"))
            .transforms(generateUserTransformations("..creator"))
            .transforms(generateUserTransformations("..closed_by"))
            .build();

    static final Endpoint ISSUE_COMMENTS = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/issues/{issue_number}/comments")
            .allowedQueryParams(issueCommentsQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..url")
                    .jsonPath("$..message")
                    .jsonPath("$..body")
                    .build())
            .transforms(generateUserTransformations("..user"))
            .build();

    static final Endpoint ISSUE_EVENTS = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/issues/{issue_number}/events")
            .allowedQueryParams(commonAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..name")
                    .jsonPath("$..url")
                    .jsonPath("$..body")
                    .jsonPath("$..rename")
                    .jsonPath("$..name")
                    .jsonPath("$..message")
                    .jsonPath("$..files")
                    .jsonPath("$..signature")
                    .jsonPath("$..payload")
                    .jsonPath("$..dismissalMessage")
                    .build())
            .transform(Transform.Pseudonymize.builder()
                    .jsonPath("$..email")
                    .build())
            .transforms(generateUserTransformations("..user"))
            .transforms(generateUserTransformations("..actor"))
            .transforms(generateUserTransformations("..assignee"))
            .transforms(generateUserTransformations("..assigner"))
            .transforms(generateUserTransformations("..creator"))
            .transforms(generateUserTransformations("..closed_by"))
            .transforms(generateUserTransformations("..author"))
            .transforms(generateUserTransformations("..committer"))
            .build();

    static final Endpoint ISSUE_TIMELINE = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/issues/{issue_number}/timeline")
            .allowedQueryParams(commonAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..name")
                    .jsonPath("$..url")
                    .jsonPath("$..body")
                    .jsonPath("$..rename")
                    .jsonPath("$..name")
                    .jsonPath("$..message")
                    .jsonPath("$..files")
                    .jsonPath("$..signature")
                    .jsonPath("$..payload")
                    .jsonPath("$..dismissalMessage")
                    .build())
            .transform(Transform.Pseudonymize.builder()
                    .jsonPath("$..email")
                    .build())
            .transforms(generateUserTransformations("..user"))
            .transforms(generateUserTransformations("..actor"))
            .transforms(generateUserTransformations("..assignee"))
            .transforms(generateUserTransformations("..assigner"))
            .transforms(generateUserTransformations("..creator"))
            .transforms(generateUserTransformations("..closed_by"))
            .transforms(generateUserTransformations("..author"))
            .transforms(generateUserTransformations("..committer"))
            .build();

    static final Endpoint PULL_REVIEWS = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/pulls/{pull_number}/reviews")
            .allowedQueryParams(commonAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..body")
                    .jsonPath("$..html_url")
                    .jsonPath("$..pull_request_url")
                    .build())
            .transforms(generateUserTransformations("..user"))
            .build();

    static final Endpoint PULLS = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/pulls")
            .allowedQueryParams(pullsAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..title")
                    .jsonPath("$..body")
                    .jsonPath("$..name")
                    .jsonPath("$..description")
                    .jsonPath("$..url")
                    .jsonPath("$..homepage")
                    .jsonPath("$..commit_title")
                    .jsonPath("$..commit_message")
                    .build())
            // Owner can be a user or an organization user;
            // we leave them without redact as we assume
            // that most of the repos are going to be
            // created as part of organization and not by an user
            //.transforms(generateUserTransformations("..owner"))
            .transforms(generateUserTransformations("..user"))
            .transforms(generateUserTransformations("..actor"))
            .transforms(generateUserTransformations("..assignee"))
            .transforms(generateUserTransformations("..assignees[*]"))
            .transforms(generateUserTransformations("..requested_reviewers[*]"))
            .transforms(generateUserTransformations("..creator"))
            .transforms(generateUserTransformations("..merged_by"))
            .transforms(generateUserTransformations("..closed_by"))
            .transforms(generateUserTransformations("..enabled_by"))
            .build();

    static final Endpoint PULL = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/pulls/{pull_number}")
            .transform(Transform.Redact.builder()
                    .jsonPath("$..title")
                    .jsonPath("$..body")
                    .jsonPath("$..name")
                    .jsonPath("$..description")
                    .jsonPath("$..homepage")
                    .jsonPath("$..commit_title")
                    .jsonPath("$..commit_message")
                    .build())
            // Owner can be a user or an organization user;
            // we leave them without redact as we assume
            // that most of the repos are going to be
            // created as part of organization and not by an user
            //.transforms(generateUserTransformations("..owner"))
            .transforms(generateUserTransformations("..user"))
            .transforms(generateUserTransformations("..actor"))
            .transforms(generateUserTransformations("..assignee"))
            .transforms(generateUserTransformations("..assignees[*]"))
            .transforms(generateUserTransformations("..requested_reviewers[*]"))
            .transforms(generateUserTransformations("..creator"))
            .transforms(generateUserTransformations("..merged_by"))
            .transforms(generateUserTransformations("..closed_by"))
            .transforms(generateUserTransformations("..enabled_by"))
            .build();

    static final Endpoint REPOSITORIES = Endpoint.builder()
            .pathTemplate("/orgs/{org}/repos")
            .allowedQueryParams(repoListAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..description")
                    .jsonPath("$..homepage")
                    .build())
            .build();

    static final Endpoint COMMIT_COMMENT_REACTIONS = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/comments/{comment_id}/reactions")
            .allowedQueryParams(commonAllowedQueryParameters)
            .transforms(generateUserTransformations("..user"))
            .build();

    static final Endpoint ISSUE_REACTIONS = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/issues/{issue_number}/reactions")
            .allowedQueryParams(commonAllowedQueryParameters)
            .transforms(generateUserTransformations("..user"))
            .build();

    static final Endpoint ISSUE_COMMENT_REACTIONS = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/issues/comments/{comment_id}/reactions")
            .allowedQueryParams(commonAllowedQueryParameters)
            .transforms(generateUserTransformations("..user"))
            .build();

    static final Endpoint REPO_EVENTS = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/events")
            .allowedQueryParams(commonAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..display_login")
                    .jsonPath("$..avatar_url")
                    .jsonPath("$..gravatar_id")
                    .jsonPath("$..html_url")
                    .jsonPath("$..name")
                    .jsonPath("$..url")
                    .jsonPath("$..message")
                    .jsonPath("$..description")
                    .jsonPath("$..body")
                    .jsonPath("$..title")
                    .build())
            .transform(Transform.Pseudonymize.builder()
                    .jsonPath("$..author.email")
                    .build())
            .transforms(generateUserTransformations("..owner"))
            .transforms(generateUserTransformations("..user"))
            .transforms(generateUserTransformations("..actor"))
            .transforms(generateUserTransformations("..assignee"))
            .transforms(generateUserTransformations("..assignees[*]"))
            .transforms(generateUserTransformations("..requested_reviewers[*]"))
            .transforms(generateUserTransformations("..creator"))
            .transforms(generateUserTransformations("..merged_by"))
            .transforms(generateUserTransformations("..closed_by"))
            .build();

    static final Endpoint REPO_COMMITS = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/commits")
            .allowedQueryParams(commitsAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..name")
                    .jsonPath("$..message")
                    .jsonPath("$..files")
                    .jsonPath("$..signature")
                    .jsonPath("$..payload")
                    .build())
            .transform(Transform.Pseudonymize.builder()
                    .jsonPath("$.commit..email")
                    .build())
            .transforms(generateUserTransformations("..author"))
            .transforms(generateUserTransformations("..committer"))
            .build();

    static final Endpoint REPO_BRANCHES = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/branches")
            .allowedQueryParams(repoBranchesAllowedQueryParameters)
            .build();

    static final Endpoint PULL_COMMITS = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/pulls/{pull_number}/commits")
            .allowedQueryParams(commonAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..name")
                    .jsonPath("$..message")
                    .jsonPath("$..files")
                    .jsonPath("$..signature")
                    .jsonPath("$..payload")
                    .build())
            .transform(Transform.Pseudonymize.builder()
                    .jsonPath("$.commit..email")
                    .build())
            .transforms(generateUserTransformations("..author"))
            .transforms(generateUserTransformations("..committer"))
            .build();

    static final Endpoint PULL_COMMENTS = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/pulls/{pull_number}/comments")
            .allowedQueryParams(pullsCommentsQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..path")
                    .jsonPath("$..diff_hunk")
                    .jsonPath("$..body")
                    .build())
            .transforms(generateUserTransformations("..user"))
            .build();

    static final Endpoint PULL_REVIEW_COMMENTS = Endpoint.builder()
            .pathTemplate("/repos/{owner}/{repo}/pulls/{pull_number}/reviews/{review_id}/comments")
            .allowedQueryParams(commonAllowedQueryParameters)
            .transform(Transform.Redact.builder()
                    .jsonPath("$..path")
                    .jsonPath("$..diff_hunk")
                    .jsonPath("$..body")
                    .build())
            .transforms(generateUserTransformations("..user"))
            .build();

    @VisibleForTesting
    static final RESTRules GITHUB = Rules2.builder()
            .endpoint(ORG_MEMBERS)
            .endpoint(GRAPHQL_FOR_USERS)
            .endpoint(ORG_TEAMS)
            .endpoint(ORG_TEAM_MEMBERS)
            .endpoint(ORG_AUDIT_LOG)
            .endpoint(REPOSITORIES)
            .endpoint(REPO_BRANCHES)
            .endpoint(REPO_COMMITS)
            .endpoint(REPO_COMMIT)
            .endpoint(REPO_EVENTS)
            .endpoint(COMMIT_COMMENT_REACTIONS)
            .endpoint(ISSUE)
            .endpoint(ISSUES)
            .endpoint(ISSUE_COMMENTS)
            .endpoint(ISSUE_EVENTS)
            .endpoint(ISSUE_TIMELINE)
            .endpoint(ISSUE_REACTIONS)
            .endpoint(ISSUE_COMMENT_REACTIONS)
            .endpoint(PULL_REVIEWS)
            .endpoint(PULLS)
            .endpoint(PULL_COMMENTS)
            .endpoint(PULL_REVIEW_COMMENTS)
            .endpoint(PULL_COMMITS)
            .endpoint(PULL)
            .build();

    public static final Map<String, RESTRules> RULES_MAP =
            ImmutableMap.<String, RESTRules>builder()
                    .put("github", GITHUB)
                    .build();

    private static List<Transform> generateUserTransformations(String prefix) {
        return Arrays.asList(
                Transform.Redact.builder()
                        .jsonPath(String.format("$%s.avatar_url", prefix))
                        .jsonPath(String.format("$%s.gravatar_id", prefix))
                        .jsonPath(String.format("$%s.url", prefix))
                        .jsonPath(String.format("$%s.html_url", prefix))
                        .jsonPath(String.format("$%s.followers_url", prefix))
                        .jsonPath(String.format("$%s.following_url", prefix))
                        .jsonPath(String.format("$%s.gists_url", prefix))
                        .jsonPath(String.format("$%s.starred_url", prefix))
                        .jsonPath(String.format("$%s.subscriptions_url", prefix))
                        .jsonPath(String.format("$%s.organizations_url", prefix))
                        .jsonPath(String.format("$%s.repos_url", prefix))
                        .jsonPath(String.format("$%s.events_url", prefix))
                        .jsonPath(String.format("$%s.received_events_url", prefix))
                        .build(),
                Transform.Pseudonymize.builder()
                        .jsonPath(String.format("$%s.login", prefix))
                        .jsonPath(String.format("$%s.id", prefix))
                        .jsonPath(String.format("$%s.node_id", prefix))
                        .jsonPath(String.format("$%s.email", prefix))
                        .build()
        );
    }

    private static JsonSchemaFilterUtils.JsonSchemaFilter jsonSchemaForUserQueryResult() {
        return JsonSchemaFilterUtils.JsonSchemaFilter.builder()
                .type("object")
                // Using LinkedHashMap to keep the order to support same
                // YAML serialization result
                .properties(new LinkedHashMap<String, JsonSchemaFilterUtils.JsonSchemaFilter>() {{ //req for java8-backwards compatibility
                    put("data", JsonSchemaFilterUtils.JsonSchemaFilter.<String, RESTRules>builder()
                            .type("object")
                            .properties(new LinkedHashMap<String, JsonSchemaFilterUtils.JsonSchemaFilter>() {{
                                put("organization", JsonSchemaFilterUtils.JsonSchemaFilter.builder()
                                        .type("object")
                                        .properties(new LinkedHashMap<String, JsonSchemaFilterUtils.JsonSchemaFilter>() {{
                                            put("samlIdentityProvider", jsonSchemaForSamlIdentityProvider());
                                        }}).build());
                            }}).build());
                    put("errors", jsonSchemaForErrors());
                }}).build();
    }

    private static JsonSchemaFilterUtils.JsonSchemaFilter jsonSchemaForSamlIdentityProvider() {
        return JsonSchemaFilterUtils.JsonSchemaFilter.builder()
                .type("object")
                .properties(new LinkedHashMap<String, JsonSchemaFilterUtils.JsonSchemaFilter>() {{
                    put("externalIdentities", jsonSchemaForExternalIdentities());
                }}).build();
    }

    private static JsonSchemaFilterUtils.JsonSchemaFilter jsonSchemaForExternalIdentities() {
        return JsonSchemaFilterUtils.JsonSchemaFilter.builder()
                .type("object")
                .properties(new LinkedHashMap<String, JsonSchemaFilterUtils.JsonSchemaFilter>() {{
                    put("pageInfo", jsonSchemaForPageInfo());
                    put("edges", jsonSchemaForEdge());
                }}).build();
    }

    private static JsonSchemaFilterUtils.JsonSchemaFilter jsonSchemaForPageInfo() {
        return JsonSchemaFilterUtils.JsonSchemaFilter.builder()
                .type("object")
                .properties(new LinkedHashMap<String, JsonSchemaFilterUtils.JsonSchemaFilter>() {{ //req for java8-backwards compatibility
                    put("hasNextPage", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("boolean").build());
                    put("endCursor", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("string").build());
                }})
                .build();
    }

    private static JsonSchemaFilterUtils.JsonSchemaFilter jsonSchemaForEdge() {
        return JsonSchemaFilterUtils.JsonSchemaFilter.builder()
                .type("array")
                .items(JsonSchemaFilterUtils.JsonSchemaFilter.builder()
                        .type("object")
                        .properties(new LinkedHashMap<String, JsonSchemaFilterUtils.JsonSchemaFilter>() {{ //req for java8-backwards compatibility
                            put("node", jsonSchemaForNode());
                        }})
                        .build())
                .build();
    }

    private static JsonSchemaFilterUtils.JsonSchemaFilter jsonSchemaForNode() {
        return JsonSchemaFilterUtils.JsonSchemaFilter.builder()
                .type("object")
                .properties(new LinkedHashMap<String, JsonSchemaFilterUtils.JsonSchemaFilter>() {{ //req for java8-backwards compatibility
                    put("guid", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("string").build());
                    put("samlIdentity", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("object").properties(new LinkedHashMap<String, JsonSchemaFilterUtils.JsonSchemaFilter>() {{
                        put("nameId", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("string").build());
                    }}).build());
                    put("user", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("object").properties(new LinkedHashMap<String, JsonSchemaFilterUtils.JsonSchemaFilter>() {{
                        put("login", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("string").build());
                    }}).build());
                }})
                .build();
    }

    private static JsonSchemaFilterUtils.JsonSchemaFilter jsonSchemaForErrors() {
        return JsonSchemaFilterUtils.JsonSchemaFilter.<String, RESTRules>builder()
                .type("array")
                .items(JsonSchemaFilterUtils.JsonSchemaFilter.builder()
                        .type("object")
                        .properties(new LinkedHashMap<String, JsonSchemaFilterUtils.JsonSchemaFilter>() {{ //req for java8-backwards compatibility
                            put("type", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("string").build());
                            put("path", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("array")
                                    .items(JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("string").build()).build());
                            put("locations", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("array")
                                    .items(JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("object")
                                            .properties(new LinkedHashMap<String, JsonSchemaFilterUtils.JsonSchemaFilter>() {
                                                {
                                                    put("line", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("integer").build());
                                                    put("column", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("integer").build());
                                                }
                                            }).build()).build());
                            put("message", JsonSchemaFilterUtils.JsonSchemaFilter.builder().type("string").build());
                        }}).build()).build();
    }
}