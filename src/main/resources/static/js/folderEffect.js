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
  	
  	