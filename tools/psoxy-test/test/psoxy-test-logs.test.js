import test from 'ava';
import * as td from 'testdouble';

let aws, gcp, psoxyTestLogs;
test.beforeEach(async (t) => {
  // double test dependencies 
  aws = (await td.replaceEsm('../lib/aws.js')).default;
  gcp = (await td.replaceEsm('../lib/gcp.js')).default;
  //  import subject under test
  psoxyTestLogs = (await import('../psoxy-test-logs.js')).default;
});

test.afterEach(() => td.reset());

test('Psoxy Logs: invalid options', async (t) => {
  await t.throwsAsync(async () => psoxyTestLogs({}), { instanceOf: Error });
  // GCP: missing functionName
  await t.throwsAsync(async () => psoxyTestLogs({
    projectId: 'foo', 
  }), { instanceOf: Error });
  // AWS: missing role
  await t.throwsAsync(async () => psoxyTestLogs({
    logGroupName: 'foo', 
  }), { instanceOf: Error });
});

test('Psoxy Logs: GCP valid options', async (t) => {
  const gcpOptions = {projectId: 'foo', functionName: 'bar'};
  td.when(gcp.getLogs(td.matchers.contains(gcpOptions))).thenResolve([]);

  const logs = await psoxyTestLogs(gcpOptions);
  t.deepEqual(logs, []);
});

test('Psoxy Logs: AWS valid options', async (t) => {
  const awsOptions = {role: 'foo', logGroupName: 'bar', region: 'baz'}
  
  td.when(aws.createCloudWatchClient(
    td.matchers.contains(awsOptions.role),
    td.matchers.contains(awsOptions.region)
  )).thenReturn({});
  
  td.when(aws.getLogStreams(
    td.matchers.contains(awsOptions),
    td.matchers.isA(Object), // cloudwatch client
  )).thenResolve({
    "$metadata": {
      httpStatusCode: 200
    },
    logStreams: [{
      logStreamName: 'foo',
    }],
  });

  td.when(aws.getLogEvents(
    td.matchers.contains(awsOptions),
    td.matchers.isA(Object), // cloudwatch client
  )).thenResolve({
    "$metadata": {
      httpStatusCode: 200
    },
    events: []
  });

  td.when(aws.parseLogEvents(td.matchers.isA(Array))).thenReturn([])

  const logs = await psoxyTestLogs(awsOptions);
  t.like(logs, {events: []});
});