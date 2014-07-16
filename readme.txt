1) fix drag drop: you need to be able to drop from multiple trees (e.g. from the repository and structure itself)
V	2) inject service runtime into pojos
3) add "value..." to the getIcon > can do lists, optionals,...
	> use css instead of hardcoded?
4) reuse the maven repository!!!!
	> this repository is a maven endpoint
	if you deploy an artifact to it, it is put in that position and exposed (read-only)
	uses same read-only logic as say a webservice
	you can add types (beans) and services
	can use xmlrootelement to indicate a bean? what for a service? the webservice one?
	> this is easier for java-driven teams
	> is nice to have a fully featured java development environment!
	> allows multiple versions (snapshots are overwritten, different versions are kept)
	> allows me to write common services etc in java
for java teams: gain deployment, versioning, webservices, easy mapping etc