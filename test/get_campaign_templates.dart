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

  settings.save();

  var resultFile = 'templates.json';

  'wget --output-document=$resultFile "https://${settings[fqdnKey]}/servicemanager/rest/CampaignAPI/getCampaignTemplateList?apiKey=${settings[apiKeyName]}"'
      .start(progress: Progress.devNull());

  var json =
      jsonDecode(read(resultFile).toList().join('\n')) as Map<String, dynamic>;

  /// print(json['entities']);

  var templates = json['entities'] as List<dynamic>;
  // print(templates);

  print('Templates');
  for (var template in templates) {
    var id = template['id'] as int;
    var name = template['name'] as String;
    print('id: $id name: $name');
  }
}

void showUsage(ArgParser parser) {
  print('Usage: get_campaigns_templates.dart');
  print(parser.usage);
  exit(1);
}
