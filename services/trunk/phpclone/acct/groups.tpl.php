<?
global $Secure_Url;
?><script type='text/javascript'>
var user = { accid: '<?=$accid?>' };
</script>
<style type='text/css'>
  #addGroupMember {
    margin: 10px 0px;
  }
  #addGroupMember input#addgroupMemberButton { 
    vertical-align: middle;
    position: relative;
  }
  #addGroupMember *, #groupNameHeader * {
    vertical-align: middle;
  }
  #addGroupMember input#maccid, input#groupName { 
    font-size: 11px;
    width: 12em; 
    margin: 0px 0.5em 0px 0.5em;
    font-weight: normal;
  } 
  #groupContainer div.yui-dt-col-accid {
    text-align: center;
  }
  #groupContainer img.clickable{
    cursor: pointer;
  }
  #groupmsg {
    padding-left: 2em;
    color: orange;
  }
  #groupContainer table {
    background-color: white;
    border-collapse: collapse;
    width: 100%;
  }
  table tbody tr.highlight, table tbody tr.highlight td {
    background-color: #FFD490 !important;
  }
  .yui-dialog .bd table th {
    text-align: right;
  }
  .yui-dialog .bd table td,
  .yui-dialog .bd table th {
    padding: 5px 10px;
    border: solid 1px #444;
  }
  .yui-dialog .bd table {
    margin: 0px 100px;
    width: 300px;
    background-color: white;
    border-collapse: collapse;
  }
  .updatingMsg, #updatedMsg {
    font-weight: normal;
    color: orange;
    font-size: 11px;
  }
  #updatedMsg {
    display: none;
  }
  #groupAcctId h4 {
    display: inline;
  }
  #groupAcctId {
    position: absolute;
    right: 20px;
    font-size: 11px;
  }
  #inviteEmailsDlg ol {
      margin-left: 60px;
  }
  #inviteEmailsDlg .inviteEmail {
    display: inline;
    margin: 0.5em 0px 0px 0px;
    width: 20em;
  }
  .inviteEmail.pending {
      /* background-color: #f2f2f2; */
      color: #ccc;
  }
  table tr.pending {
      color: #aaa;
  }
  .hidden {
    display: none;
  }
  #changeLogoFields * {
    vertical-align: middle;
  }
  
</style>
<br/>
  <h4 > http://www.medcommons.net/<?=htmlentities($practice)?>
  </h4> 

  <p>The MedCommons Groupid is: <span><?=pretty_mcid($active_group_accid)?></span></p>
  <div id='addGroupMember'>
  <input type='image' id='inviteGroupMemberButton' value='Invite'
         title='Click to invite a new person to the group by Email'
         src='images/invite_button.png' onmousedown='this.style.top="2px";' onmouseup='this.style.top="1px"; this.blur();'/>
  <span id='groupmsg'></span> Invite Others to this Group

  </div>
  <hr/>
  <div id='groupContainer'>
  </div>
  <div style='margin-top: 10px;'>
   <p>Send imaging automatically to your DICOM workstation or PACS 
        <button onclick="location.href='dod_poller.php'">Install Polling DDL</button></p>
   
   <?if(is_feature_enabled("group.uploadPage")):?>
   <p><input type='checkbox' name='allowPublicUpload' style='position: relative; top: 2px;' onclick='enableGroupUploads(this.checked);'
       <?if($user->practice->enable_uploads):?>checked='true'<?endif;?>
       title='Place a link to this page on your web site or send them by email to enable others to send DICOM to your Group'/> 
        Receive health records with a practice dropbox 
        <button onclick="location.href='<?=$Secure_Url?>/<?=$active_group_accid?>/upload'" 
            title='Place a link to this page on your web site or send them by email to enable others to send DICOM to your Group'>
            <?=hsc($user->practice->practicename)?> Dropbox</button>
   </p>
    <?endif;?>
   <?if(is_feature_enabled("group.customLogo")):?>
   <p class='checkboxRow'><input type='checkbox' name='groupLogoCheckBox' style='position: relative; top: 2px;' onclick='enableGroupLogo(this.checked);'
       <?if($user->practice->logo_url):?>checked='true'<?endif;?>
       title='This logo will be displayed on screens associated with your group'/> 
        Show a custom logo in header of your Dropbox and pages associated with this group&nbsp;
   </p>
        <div id='changeLogoFields'
		  <?if(!$user->practice->logo_url):?>
		  class='hidden'
		  <?endif;?>
        >
		&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
		Image URL: <input type='text' id='groupLogo' name='groupLogo' size='80' 
		  value='<?=htmlentities($user->practice->logo_url, ENT_QUOTES)?>'/>
		  &nbsp;
		  <input type='image' id='changeLogoButton' value='Change'  
		         title='Click to update the group logo' 
		         src='images/change_button.png' 
		         onmousedown='this.style.top="2px";' onmouseup='this.style.top="1px"; this.blur();'/>
		         
		  <span id='grouplogoUpdate' class='updatingMsg hidden'>Updating ...</span>
		  
	  </div>
    
    <?endif;?>
    
   <?if(is_feature_enabled("group.apiSigning")):?>
   <p class='checkboxRow'><input type='checkbox' name='apisigning' id='apisigning' style='position: relative; top: 2px;' onclick='enableAPIKeys(this.checked);'
       <?if(isset($apiKeys)):?>checked='true'<?endif;?>
       title=''/> 
        &nbsp;
        Enable access to group functions by external applications using signed API calls 
        <a <?if(!isset($apiKeys)):?>class='hidden'<?endif;?> id='keysLink'
            href='javascript:enableAPIKeys(true)'>Show Keys</a>
   </p>
   <?endif;?>
    
  </div>
</div>
