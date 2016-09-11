/*
 Copyright 2016 Jacopo Rigoli

 Licensed under the Apache License, Version 2.0 (the "License");
 you may not use this file except in compliance with the License.
 You may obtain a copy of the License at

 http://www.apache.org/licenses/LICENSE-2.0

 Unless required by applicable law or agreed to in writing, software
 distributed under the License is distributed on an "AS IS" BASIS,
 WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 See the License for the specific language governing permissions and
 limitations under the License.
 */
var folderOpen = null;

function folderClicked(folderButton,folderName){
	tmpFolderOpen = folderOpen;
	if(folderOpen!=null) hideFolderContent();
	if(folderName != tmpFolderOpen) showFolderContent(folderButton,folderName);
}

function showFolderContent(folderButton,folderName){
	folder = document.getElementById(folderName);
	folder.className = "visible";

	//folderButton.style.opacity = 1;

	$("body").find(".folder").not(folderButton).each(function() {
		$(this).animate({
			opacity: 0.20
		}, "fast");
	});

	folderOpen = folderName;
}

function hideFolderContent(){
	$("body").find(".folder").each(function() {
		$(this).animate({
			opacity: 0.9
		}, "fast");
	});

	folder = document.getElementById(folderOpen);
	folder.className = "";
	folderOpen = null;
}
