import test from 'ava';
import * as td from 'testdouble';
import { 
  executeWithRetry,
  resolveHTTPMethod, 
  transformSpecWithResponse 
} from '../lib/utils.js';
import spec from '../data-sources/spec.js';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);
// Unorthodox approach: load actual JSON response examples used by Psoxy backend
const slackResponse = require('../../../java/core/src/test/resources/api-response-examples/slack/discovery-enterprise-info.json');
const calendarEventsResponse = require('../../../java/core/src/test/resources/api-response-examples/g-workspace/calendar/events.json');

test('Transform data sources spec with API responses: param replacement', (t) => {
  const unknownSpec = spec['foo'];
  let result = transformSpecWithResponse(unknownSpec, slackResponse);
  t.deepEqual(result, {});

  const slackSpec = spec['slack-discovery-api'];

  result = transformSpecWithResponse(slackSpec, {});
  t.deepEqual(result, slackSpec);

  result = transformSpecWithResponse(slackSpec, slackResponse);
  const workspaceConversationsEndpoint = result.endpoints.find(
    (endpoint) => endpoint.name === 'Workspace Conversations'
  );

  // `team` param replacement
  t.is(workspaceConversationsEndpoint.params.team, slackResponse.enterprise.teams[0].id);
});

test('Transform data sources spec with API responses: path replacement', (t) => {
  const gcalSpec = spec['gcal'];
  const result = transformSpecWithResponse(gcalSpec, calendarEventsResponse);

  // [event_id] path replacement
  const eventEndpoint = result.endpoints.find((endpoint) => endpoint.name === 'Event');
  t.true(eventEndpoint.path.endsWith(calendarEventsResponse.items[0].id));
});

test('Resolve HTTP method', (t) => {
  // Endpoint without method specified, defaults to 'GET'
  t.is(resolveHTTPMethod('/gmail/v1/users/me/messages'), 'GET');
  // Unknown endpoint (not in spec), defaults to 'GET'
  t.is(resolveHTTPMethod('/foo/bar'), 'GET');
  t.is(resolveHTTPMethod('/2/team/members/list_v2'), 'POST');
});

test('Execute with retry: make "n" attempts if conditions met', async (t) => {
  const work = td.func('work');
  const onErrorStop = td.func('onErrorStop');
  const logger = td.object({ info: () => {} });
  const attempts = 2;
  const delay = 1; // timeout, make test to not wait

  td.when(work()).thenThrow(new Error());
  td.when(onErrorStop(td.matchers.isA(Error))).thenReturn(false);

  t.falsy(await executeWithRetry(work, onErrorStop, logger, attempts, delay), 
    'Undefined result after retries');

  td.verify(work, { times: attempts });
});

test('Execute with retry: does not retry if worker returns value', async (t) => {
  const work = td.func('work');
  td.when(work()).thenReturn(true);

  t.is(await executeWithRetry(work), true);
});

test('Execute with retry: stops on error callback', async (t) => {
  const work = td.func('work');
  const onErrorStop = td.func('onErrorStop');

  td.when(work()).thenThrow(new Error());
  td.when(onErrorStop(td.matchers.isA(Error))).thenReturn(true);

  await t.throwsAsync(async () => {
		await executeWithRetry(work, onErrorStop);
	}, {instanceOf: Error});
});