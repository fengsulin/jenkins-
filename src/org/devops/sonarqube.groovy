package org.devops

// sonar官方的文档获取接口信息
/**
 //查找项目
 api/projects/search?projects=${projectName}"

 //创建项目
 api/projects/create?name=${projectName}&project=${projectName}"

 //更新语言规则集
 api/qualityprofiles/add_project?language=${language}&qualityProfile=${qualityProfile}&project=${projectName}"

 //项目授权
 api/permissions/apply_template?projectKey=${projectKey}&templateName=${templateName}"

 //更新质量阈
 api/qualitygates/select?projectKey=${projectKey}&gateId=${gateId}"

 */

//封装HTTP

def HttpReq(reqType,reqUrl,reqBody){
    def sonarServer = "http://192.168.230.135:30090/api"

    result = httpRequest authentication: 'sonar-admin-user',
            httpMode: reqType,
            contentType: "APPLICATION_JSON",
            consoleLogResponseBody: true,
            ignoreSslErrors: true,
            requestBody: reqBody,
            url: "${sonarServer}/${reqUrl}"
    //quiet: true

    return result
}


//获取Sonar质量阈状态
def GetProjectStatus(projectName){
    apiUrl = "project_branches/list?project=${projectName}"
    response = HttpReq("GET",apiUrl,'')

    response = readJSON text: """${response.content}"""
    result = response["branches"][0]["status"]["qualityGateStatus"]

    //println(response)

    return result
}

//获取Sonar项目扫描结果(多分支)
def GetProjectResult(projectName,branchName){
    apiUrl = "qualitygates/project_status?projectKey=${projectName}&branch=${branchName}"
    response = HttpReq("GET",apiUrl,'')

    response = readJSON text: """${response.content}"""
    result = response["projectStatus"]["status"]

    //println(response)

    return response
}

// 获取Sonar项目扫描状态(多分支)
def GetProjectStatus(projectName,branchName){
    response = GetProjectResult(projectName,branchName)
    result = response["projectStatus"]["status"]
    println(result)
    return result
}

//搜索Sonar项目
def SearchProject(projectName){
    apiUrl = "projects/search?projects=${projectName}"
    response = HttpReq("GET",apiUrl,'')

    response = readJSON text: """${response.content}"""
    result = response["paging"]["total"]

    if(result.toString() == "0"){
        return "false"
    } else {
        return "true"
    }
}

//创建Sonar项目
def CreateProject(projectName){
    apiUrl =  "projects/create?name=${projectName}&project=${projectName}"
    response = HttpReq("POST",apiUrl,'')
    println(response)
}

//配置项目质量规则

def ConfigQualityProfiles(projectName,lang,qpname){
    apiUrl = "qualityprofiles/add_project?language=${lang}&project=${projectName}&qualityProfile=${qpname}"
    response = HttpReq("POST",apiUrl,'')
    println(response)
}


//获取质量阈ID
def GetQualityGateId(gateName){
    apiUrl= "qualitygates/show?name=${gateName}"
    response = HttpReq("GET",apiUrl,'')
    response = readJSON text: """${response.content}"""
    result = response["id"]

    return result
}

//配置项目质量阈
def ConfigQualityGates(projectName,gateName){
    gateId = GetQualityGateId(gateName)
    apiUrl = "qualitygates/select?gateId=${gateId}&projectKey=${projectName}"
    response = HttpReq("POST",apiUrl,'')
    println(response)
}
