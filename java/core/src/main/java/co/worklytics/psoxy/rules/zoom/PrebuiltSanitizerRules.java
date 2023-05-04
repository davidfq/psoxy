package co.worklytics.psoxy.rules.zoom;

import com.avaulta.gateway.rules.Endpoint;
import co.worklytics.psoxy.rules.RESTRules;
import co.worklytics.psoxy.rules.Rules2;
import com.avaulta.gateway.rules.transforms.Transform;
import com.avaulta.gateway.pseudonyms.PseudonymEncoder;
import com.google.common.collect.ImmutableMap;

import java.util.Map;

/**
 * Prebuilt sanitization rules for Zoom API responses
 */
public class PrebuiltSanitizerRules {

    static final Rules2 MEETINGS_ENDPOINTS = Rules2.builder()
        // Users' meetings
        // https://marketplace.zoom.us/docs/api-reference/zoom-api/meetings/meetings
        .endpoint(Endpoint.builder()
            .pathTemplate("/v2/users/{userId}/meetings")
            .transform(Transform.Pseudonymize.builder()
                .jsonPath("$.meetings[*]['host_id','host_email']")
                .build())
            .transform(Transform.Redact.builder()
                .jsonPath("$.meetings[*]['topic','join_url','start_url','agenda']")
                .build())
            .build()
        )
        // Meeting details
        // https://marketplace.zoom.us/docs/api-reference/zoom-api/meetings/meeting
        .endpoint(Endpoint.builder()
            .pathTemplate("/v2/meetings/{meetingId}")
            .transform(Transform.Pseudonymize.builder()
                .jsonPath("$.['host_id','host_email']")
                .build())
            .transform(Transform.Redact.builder()
                .jsonPath("$.['topic','settings','agenda','custom_keys']")
                .jsonPath("$.['password','h323_password','pstn_password','encrypted_password','join_url','start_url']")
                .build())
            .build()
        )
        // List past meeting instances
        // https://marketplace.zoom.us/docs/api-reference/zoom-api/meetings/pastmeetings
        .endpoint(Endpoint.builder()
            .pathTemplate("/v2/past_meetings/{meetingId}/instances")
            .build())
        // Past meeting details
        // https://marketplace.zoom.us/docs/api-reference/zoom-api/meetings/pastmeetingdetails
        .endpoint(Endpoint.builder()
            .pathTemplate("/v2/past_meetings/{meetingId}")
            .transform(Transform.Pseudonymize.builder()
                .jsonPath("$.['host_id','user_email','host_email']")
                .build()
            )
            .transform(Transform.Redact.builder()
                // we don't care about user's name
                .jsonPath("$.['user_name','topic','agenda']")
                .build())
            .build())
        // Past meeting participants
        // https://marketplace.zoom.us/docs/api-reference/zoom-api/meetings/pastmeetingparticipants
        .endpoint(Endpoint.builder()
            .pathTemplate("/v2/past_meetings/{meetingId}/participants")
            .transform(Transform.Pseudonymize.builder()
                .jsonPath("$.participants[*]['id','user_email','pmi']")
                .build()
            )
            .transform(Transform.Redact.builder()
                // we don't care about user's name
                .jsonPath("$.participants[*]['name','registrant_id']")
                .build())
            .build())
        .build();


    static final Rules2 REPORT_ENDPOINTS = Rules2.builder()
        // List meetings
        // https://marketplace.zoom.us/docs/api-reference/zoom-api/methods#operation/reportMeetings
        .endpoint(Endpoint.builder()
            .pathTemplate("/v2/report/users/{accountId}/meetings")
            .transform(Transform.Pseudonymize.builder()
                .jsonPath("$.meetings[*]['host_id','user_email','host_email']")
                .build()
            )
            .transform(Transform.Redact.builder()
                // we don't care about user's name
                .jsonPath("$.meetings[*]['user_name','topic','custom_keys','tracking_fields']")
                .build())
            .build())
        // Past meeting details
        // https://marketplace.zoom.us/docs/api-reference/zoom-api/methods#operation/reportMeetingDetails
        .endpoint(Endpoint.builder()
            .pathTemplate("/v2/report/meetings/{meetingId}")
            .transform(Transform.Pseudonymize.builder()
                .jsonPath("$.['host_id','user_email','host_email']")
                .build()
            )
            .transform(Transform.Redact.builder()
    // we don't care about user's name
                .jsonPath("$.['user_name','topic','custom_keys','tracking_fields']")
                .build())
        .build())
        // Past meeting participants
        // https://marketplace.zoom.us/docs/api-reference/zoom-api/methods#operation/reportMeetingParticipants
        .endpoint(Endpoint.builder()
            .pathTemplate("/v2/report/meetings/{meetingId}/participants")
            .transform(Transform.Pseudonymize.builder()
                .jsonPath("$.participants[*]['id','user_email','user_id','pmi']")
                .build()
            )
            .transform(Transform.Redact.builder()
    // we don't care about user's name
                .jsonPath("$.participants[*]['name','registrant_id','display_name']")
                .build())
        .build())
        .build();




    static final Rules2 USERS_ENDPOINTS = Rules2.builder()
        // List users
        // https://marketplace.zoom.us/docs/api-reference/zoom-api/users/users
        .endpoint(Endpoint.builder()
            .pathTemplate("/v2/users")
            .transform(Transform.Pseudonymize.builder()
                .jsonPath("$.users[*]['email','phone_number']")
                .build()
            )
            .transform(Transform.Pseudonymize.builder()
                .includeReversible(true) // need to reverse when requesting meetings by user to iterate
                .jsonPath("$.users[*]['id','pmi']")
                .encoding(PseudonymEncoder.Implementations.URL_SAFE_TOKEN)
                .build()
            )
            .transform(Transform.Redact.builder()
                // we don't care about names, profile pic, employee_unique_id (when SSO info)
                .jsonPath("$.users[*]['display_name','first_name','last_name','pic_url','employee_unique_id']")
                .build()
            )
            .build())
        .build();

    static final Rules2 ZOOM = USERS_ENDPOINTS
        .withAdditionalEndpoints(MEETINGS_ENDPOINTS.getEndpoints())
        .withAdditionalEndpoints(REPORT_ENDPOINTS.getEndpoints());


    static public final Map<String, RESTRules> ZOOM_PREBUILT_RULES_MAP = ImmutableMap.<String, RESTRules>builder()
        .put("zoom", ZOOM)
        .build();
}
