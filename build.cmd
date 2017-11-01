@echo off

set dir=pxene-crawler

call mvn clean package

md target\%dir%
md target\%dir%\bin
md target\%dir%\conf
md target\%dir%\spiders
md target\%dir%\spiders\common
md target\%dir%\spiders\extract
md target\%dir%\lib
md target\%dir%\work
xcopy bin target\%dir%\bin
xcopy conf target\%dir%\conf
xcopy target\pxene-crawler-project.jar target\%dir%\lib
xcopy spiders\spider_example.json target\%dir%\spiders\
xcopy spiders\spider_tpl.json target\%dir%\spiders\
