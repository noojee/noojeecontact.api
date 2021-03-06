#! /usr/bin/env dcli

import 'dart:convert';
import 'dart:io';
import 'package:dcli/dcli.dart';
import 'package:settings_yaml/settings_yaml.dart';

/// dcli script generated by:
/// dcli create get_campaigns.dart
///
/// See
/// https://pub.dev/packages/dcli#-installing-tab-
///
/// For details on installing dcli.
///

void main(List<String> args) {
  var settings = SettingsYaml.load(pathToSettings: '.settings.yaml');

  const apiKeyName = 'apiKey';

  settings[apiKeyName] =
      ask('Api Key:', defaultValue: settings[apiKeyName] as String);

  const fqdnKey = 'fqdnKey';
  settings[fqdnKey] =
      ask('Customer FQDN:', defaultValue: settings[fqdnKey] as String);

  const templateIdName = 'templateIDName';
  settings[templateIdName] =
      ask('Campaign Template ID:', defaultValue: '${settings[templateIdName]}');

  const campaignIdName = 'campaignIDName';
  settings[campaignIdName] =
      ask('Campaign ID:', defaultValue: '${settings[campaignIdName]}');

  const allocationIdName = 'allocationIdName';
  settings[allocationIdName] =
      ask('Allocation ID:', defaultValue: '${settings[allocationIdName]}');

  settings.save();

  var resultFile = 'recordings.json';

  '''wget  --output-document=$resultFile  "https://${settings[fqdnKey]}/servicemanager/rest/CampaignAPI/getCallRecordList
?fTemplateId=${settings[templateIdName]}
&campaignId=${settings[campaignIdName]}
&allocationId=${settings[allocationIdName]}
&apiKey=${settings[apiKeyName]}"'''
      .replaceAll('\n', '')
      .start(progress: Progress.devNull());

  var json =
      jsonDecode(read(resultFile).toList().join('\n')) as Map<String, dynamic>;

  /// print(json['entities']);

  var recordings = json['entities'] as List<dynamic>;
  // print(templates);

  print('Recordings');
  for (var recording in recordings) {
    var id = recording['id'] as int;
    var name = recording['name'] as String;
    print('id: $id name: $name');
  }
}

void showUsage(ArgParser parser) {
  print('Usage: get_campaigns.dart -v -prompt <a questions>');
  print(parser.usage);
  exit(1);
}
