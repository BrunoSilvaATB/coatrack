:: Script to build CoatRack using Maven in Windows systems

:: Please note:
:: - a CoatRack build requires some mail configuration parameters to be set
:: - the configuration values in this file are just dummy/fallback values for development builds

mvn clean package -Dygg.mail.sender.user="" -Dygg.mail.sender.password="" -Dygg.mail.server.url="" -Dygg.mail.server.port=0
