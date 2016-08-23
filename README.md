[![Build Status](https://travis-ci.org/DSI-Ville-Noumea/batch-migrate-sharepoint-to-alfresco.svg?branch=master)](https://travis-ci.org/DSI-Ville-Noumea/batch-migrate-sharepoint-to-alfresco)

# batch-migrate-sharepoint-to-alfresco

Batch de migration Sharepoint vers Alfresco.

Ce batch comprend un JOB :

* job importEAESharepoint pour migrer les EAE de Sharepoint vers Alfresco

# Properties

[application.properties](./src/main/resources/application.properties)

# RUN

Pour lancer le JOB **importEAESharepoint**

`java -jar batch-migrate-sharepoint-to-alfresco##1.0.jar --spring.batch.job.names=importEAESharepoint `

# Partners

We'd like to thank [Skazy.NC](http://www.skazy.nc/) team for developing for us this software.
