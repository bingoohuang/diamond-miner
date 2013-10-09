<%@page contentType="text/html;charset=UTF-8" isErrorPage="true" %>
<html>
<head><title>error</title>
    <script type="text/javascript">
        function displayErrorInfo() {
            var errorInfo = document.getElementById("errorInfo");
            errorInfo.style.display = (errorInfo.style.display == "none" ? "" : "none");
        }
    </script>
</head>
<body>
<p>internal error, pls contact the administrator.</p>

<p><a onclick="displayErrorInfo();" href="#">Check exception details</a></p>

<div id="errorInfo" style="display:none"><%=exception.getMessage()%>
</div>
</body>
</html>