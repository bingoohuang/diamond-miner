<html xmlns="http://www.w3.org/1999/xhtml">
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ page contentType="text/html; charset=UTF-8" pageEncoding="UTF-8" %>
<head>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8"/>
    <title>Diamond-Server Console</title>
</head>
<body>
<ul>
    <li><a href="config/list.jsp" target="rightFrame">Configuration</a></li>
    <li><a href="<c:url value='/admin.do?method=listUser'/>" target="rightFrame">Privileges</a></li>
    <li><a href="<c:url value='/admin.do?method=getRefuseRequestCount'/>" target="rightFrame">Rejection</a></li>
    <c:url var="logoutUrl" value="/login.do">
        <c:param name="method" value="logout"/>
    </c:url>
    <li><a href="${logoutUrl}" target="_top">Logout</a></li>
</ul>
</body>
</html>