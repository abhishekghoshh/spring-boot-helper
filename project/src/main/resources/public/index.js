$(function(){
	checkLogin();
	$("#formSignup").on('submit',function(event){
		event.preventDefault();
		var email=$("#signupEmail").val();
		var password=$("#signupPassword").val();
		var firstAns=$("#signupAns1").val();
		var secondAns=$("#signupAns2").val();
		var employee=JSON.stringify({
			email:email,
			password:password,
			firstAns:firstAns,
			secondAns:secondAns
		});
		$.ajax({
            url: '/api/signup',
            dataType: 'json',
            type: "POST",
            contentType: "application/json; charset=utf-8",
            data: employee,
            cache: false,
            timeout: 500000,
            success: function(res){
            	console.log("in success signup");
            	console.log("message is "+res.message);
            	$("#token").text(res.message);
            	checkLogin();
            },
            error: function(res){
            	console.log("in error signup");
            	console.log(res);
            	var text=res.responseText;
            	var message=JSON.parse(text);
            	$("#alertDiv").html('<div class="alert alert-danger alert-dismissible fade in"><a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a><strong id="alertMessage"></strong> </div>');
            	$("#alertMessage").text(message.message);
               	}
        });
	});
	$("#formLogin").on("submit",function(event){
		event.preventDefault();
		var id=$("#loginId").val();
		var email="";
		if(typeof(id)=="string"){
			email=id;
			id=0;
		}
		var password=$("#loginPassword").val();
		var employee=JSON.stringify({
			id:id,
			email:email,
			password:password
		});
		console.log(employee);
		$.ajax({
            url: '/api/login',
            dataType: 'json',
            type: "POST",
            contentType: "application/json; charset=utf-8",
            data: employee,
            cache: false,
            timeout: 500000,
            success: function(res){
//            	var text = res.responseText;
//            	console.log("this is response text",text);
               //console.log(jQxhr);
            	console.log("in success "+res)
            },
            error: function(res){
            	console.log("in error ");
            	
               //console.log(errorThrown.responseText);
            	console.log(res);
               var text=res.responseText;
               //checkLogin();
               if(text.includes("Token")){
            	   checkLogin();
            	   console.log("in login"+text);
               }else{
            	   var message=JSON.parse(text);
            	   console.log(message);
            	   $("#alertDivLogin").html('<div class="alert alert-danger alert-dismissible fade in"><a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a><strong id="alertMessageLogin"></strong> </div>');
            	   $("#alertMessageLogin").text(message.message);
            	   
            	   
               }
                
            }
        });
	});
	$("#logout").click(function(event){
		clearform();
		$("#updateEmployeeDiv").hide();
		$("#EmployeeDetails").hide();
		event.preventDefault();
		checkLogin();
		var header=$("#token").text();
		console.log("header is "+header);
		$.ajax({
            url: '/info/logout',
            type: "GET",
            headers: {
                'Authorization':header
                },
            cache: false,
            timeout: 500000,
            success: function(){
            	console.log("in success ");
            	checkLogin();
            },
            error: function(res){
            	console.log("in error ");
            	console.log(res);
            	checkLogin();
                
            }
        });
		
	});
	$("#updateEmployee").click(function(event){
		event.preventDefault();
		$("#updateEmployeeDiv").show();
		$("#EmployeeDetails").hide();
		setEmployeeInfo($("#updateId").val());
	});
	$("#updateForm").on("submit",function(event){
		event.preventDefault();
		var employeeInfo=JSON.stringify({
			id:$("#updateId").val(),
			email:$("#updateEmail").val(),
			designation:$("#updateDesignation").val(),
			firstName:$("#updateFirstname").val(),
			lastName:$("#updateLastname").val(),
			phone:$("#updatePhone").val()
		});
		var header=$("#token").text();
		$.ajax({
            url: '/info/updateEmployeeInfo',
            type: "PUT",
            contentType: "application/json; charset=utf-8",
            data: employeeInfo,
            headers: {
                'Authorization':header
                },
            cache: false,
            timeout: 500000,
            success: function(res){
            	console.log("in success ");
            	console.log(res);
            	$("#alertDivUpdate").html('<div class="alert alert-success alert-dismissible fade in"><a href="#" class="close" data-dismiss="alert" aria-label="close">&times;</a><strong id="alertMessageLogin"></strong> </div>');
         	   $("#alertMessageLogin").text(res);
         	   
            },
            error: function(res){
            	console.log("in error ");
            	console.log(res);}
                
            });
	});
	$("#searchEmployee").on("submit",function(event){
		event.preventDefault();
		var header=$("#token").text();
		$("#updateEmployeeDiv").hide();
		$("#EmployeeDetails").show();
		var url="info/searchEmployeeInfo/"+$("#searchKey").val();
		console.log(url);
		$.ajax({
            url: url,
            type: "GET",
            headers: {
                'Authorization':header
                },
            cache: false,
            timeout: 500000,
            success: function(res){
            	console.log("in success "+res.length);
            	var html='';
            	for(var i=0;i<res.length;i++){
            		html=html+
            		`<tr>
            		<td>${res[i].id}</td>
            		<td>${res[i].email}</td>
            		<td>${res[i].firstName}</td>
            		<td>${res[i].lastName}</td>
            		<td>${res[i].designation}</td>
            		<td>${res[i].phone}</td>
            		</tr>`;
            	}
            	$("#data").html(html);
            },
            error: function(res){
            	console.log("in error ");
            	console.log(res);
                
            }
        });
		
	});
	$("#showEmployee").click(function(event){
		event.preventDefault();
		var header=$("#token").text();
		$("#updateEmployeeDiv").hide();
		$("#EmployeeDetails").show();
		var url="info/findAllEmployeeInfo";
		console.log(url);
		$.ajax({
            url: url,
            type: "GET",
            headers: {
                'Authorization':header
                },
            cache: false,
            timeout: 500000,
            success: function(res){
            	console.log("in success "+res.length);
            	var html='';
            	for(var i=0;i<res.length;i++){
            		html=html+
            		`<tr>
            		<td>${res[i].id}</td>
            		<td>${res[i].email}</td>
            		<td>${res[i].firstName}</td>
            		<td>${res[i].lastName}</td>
            		<td>${res[i].designation}</td>
            		<td>${res[i].phone}</td>
            		</tr>`;
            	}
            	$("#data").html(html);
            },
            error: function(res){
            	console.log("in error ");
            	console.log(res);
                
            }
	});
	});
	
});
function checkLogin(){
	var employee;
	$.ajax({
		url:"api/checkLoggedin",
		cache: false,
		type:"GET",
        timeout: 500000,
        success:function(res){
        	employee=res;
        	if(employee.id==undefined){
        		$("#loggedIn").hide();
        		$("#loggedOut").show();
        		$("#loginSignup").show();
        	}
        	else{
        		getToken();
        		$("#loggedOut").hide();
        		$("#loggedIn").show();
        		$("#loginSignup").hide();
        		console.log(employee.id,employee.email);
        		$("#updateId").val(employee.id);
            	$("#updateEmail").val(employee.email);
        	}
        },
        error:function(res){
        	employee=res.responseText;
        }
	});
}
function getToken(){
	$.ajax({
		url:"api/getToken",
		cache: false,
		type:"GET",
        timeout: 500000,
        success:function(res){
        	$("#token").text(res);
        },
        error:function(res){
        	
        }
	});
	
}
function clearform(){
	$("input").val("");
}

function setEmployeeInfo(id){
	var header=$("#token").text();
	console.log("header "+header);
	var url="info/findEmployeeInfoById/"+id;
	console.log(url);
	$.ajax({
        url: url,
        type: "GET",
        headers: {
            'Authorization':header
            },
        cache: false,
        timeout: 500000,
        success: function(res){
        	console.log("in success ");
        	console.log(res);
			$("#updateDesignation").val(res.designation);
			$("#updateFirstname").val(res.firstName);
			$("#updateLastname").val(res.lastName);
			$("#updatePhone").val(res.phone);
        },
        error: function(res){
        	console.log("in error ");
        	console.log(res);
            
        }
    });
}