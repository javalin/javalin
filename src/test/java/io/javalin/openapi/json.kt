/**
 * This file contains the json definitions required for the test
 */
package io.javalin.openapi

import com.fasterxml.jackson.databind.JsonNode
import io.javalin.plugin.json.JavalinJackson
import io.swagger.v3.oas.models.OpenAPI
import org.intellij.lang.annotations.Language

fun String.formatJson(): String {
    val node = JavalinJackson.fromJson(this, JsonNode::class.java)
    return JavalinJackson.toJson(node)
}

fun OpenAPI.asJsonString(): String = JavalinJackson.toJson(this)

// This variable is needed for the jsons. That's how I can avoid the ugly """${"$"}""".
val ref = "\$ref"

@Language("JSON")
val userOpenApiSchema = """
{
   "required":[
      "name"
   ],
   "type":"object",
   "properties":{
      "name":{
         "type":"string"
      }
   }
}
""".formatJson()

@Language("JSON")
val complexExampleUsersGetResponsesJson = """
{
  "200": {
    "description": "",
    "content": {
      "application/json": {
        "schema": {
          "type": "array",
          "items": {
            "$ref": "#/components/schemas/User"
          }
        }
      }
    }
  }
}
""".formatJson()

@Language("JSON")
val provideRouteExampleJson = """
  {
    "openapi": "3.0.1",
    "info": {
      "title": "Example",
      "version": "1.0.0"
    },
    "paths": {
      "/docs/swagger.json": {
        "get": {
          "summary": "Get docs swagger.json",
          "operationId": "getDocsSwagger.json",
          "responses" : {
            "200" : {
              "description" : ""
            }
          }
        }
      },
      "/test": {
        "get": {
          "summary": "Get test",
          "operationId": "getTest"
        }
      }
    },
    "components": {}
  }
""".formatJson()

@Language("JSON")
val complexExampleJson = """
{
  "openapi": "3.0.1",
  "info": {
    "title": "Example",
    "version": "1.0.0"
  },
  "externalDocs": {
    "description": "Find more info here",
    "url": "https://external-documentation.info"
  },
  "servers": [
    {
      "url": "https://app.example",
      "description": "My example app"
    }
  ],
  "security": [
    {
      "http": []
    }
  ],
  "tags": [
    {
      "name": "user",
      "description": "User operations"
    }
  ],
  "paths": {
    "/unimplemented": {
      "get": {
        "summary": "This path is not implemented in javalin"
      }
    },
    "/user": {
      "description": "Some additional information for the /user endpoint",
      "get": {
        "tags": ["user"],
        "summary": "Get current user",
        "description": "Get a specific user",
        "operationId": "getCurrentUser",
        "responses": {
          "200": {
            "description": "Request successful",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/User"
                }
              }
            }
          }
        },
        "deprecated": true
      },
      "put": {
        "tags": ["user"],
        "summary": "Put user",
        "operationId": "putUser",
        "requestBody": {
          "description": "body description",
          "content": {
            "text/plain": {
              "schema": {
                "type": "string"
              }
            },
            "application/json": {
              "schema": {
                "$ref": "#/components/schemas/User"
              }
            },
            "application/octet-stream": {
              "schema": {
                "type": "string",
                "format": "binary"
              }
            },
            "image/png": {
              "schema": {
                "type": "string",
                "format": "binary"
              }
            }
          },
          "required": true
        }
      }
    },
    "/users/{my-path-param}": {
      "get": {
        "tags": ["user"],
        "summary": "Get users with myPathParam",
        "operationId": "getUsersWithMyPathParam",
        "parameters": [
          {
            "name": "my-path-param",
            "in": "path",
            "description": "My path param",
            "required": true,
            "schema": {
              "type": "integer",
              "format": "int32"
            }
          },
          {
            "name": "my-cookie",
            "in": "cookie",
            "description": "My cookie",
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "x-my-header",
            "in": "header",
            "description": "My header",
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "name",
            "in": "query",
            "description": "The name of the users you want to filter",
            "required": true,
            "deprecated": true,
            "allowEmptyValue": true,
            "schema": {
              "type": "string"
            }
          },
          {
            "name": "age",
            "in": "query",
            "schema": {
              "type": "integer",
              "format": "int32"
            }
          }
        ],
        "responses": $complexExampleUsersGetResponsesJson
      }
    },
    "/users2": {
      "get": {
        "tags": ["user"],
        "summary": "Get users2",
        "operationId": "getUsers2",
        "responses": $complexExampleUsersGetResponsesJson
      }
    },
    "/string": {
      "get": {
        "summary": "Get string",
        "operationId": "getString",
        "responses": {
          "200": {
            "description": "",
            "content": {
              "text/plain": {
                "schema": {
                  "type": "string"
                }
              }
            }
          }
        }
      }
    },
    "/homepage": {
      "get": {
        "summary": "Get homepage",
        "operationId": "getHomepage",
        "responses": {
          "200": {
            "description": "My Homepage",
            "content": {
              "text/html": {
                "schema": {
                  "type": "string"
                }
              }
            }
          }
        }
      }
    },
    "/upload": {
      "get": {
        "summary": "Get upload",
        "operationId": "getUpload",
        "requestBody": {
          "description": "MyFile",
          "content": {
            "multipart/form-data": {
              "schema": {
                "type": "object",
                "properties": {
                  "file": {
                    "type": "string",
                    "format": "binary"
                  }
                }
              }
            }
          },
          "required": true
        }
      }
    },
    "/uploads": {
      "get": {
        "summary": "Get uploads",
        "operationId": "getUploads",
        "requestBody": {
          "description": "MyFiles",
          "content": {
            "multipart/form-data": {
              "schema": {
                "type": "object",
                "properties": {
                  "files": {
                    "type": "array",
                    "items": {
                      "type": "string",
                      "format": "binary"
                    }
                  }
                }
              }
            }
          },
          "required": true
        }
      }
    },
    "/resources/*": {
      "get": {
        "summary": "Get resources with wildcard",
        "operationId": "getResourcesWithWildcard",
        "responses": {
          "200": {
            "description": ""
          }
        }
      }
    }
  },
  "components": {
    "schemas": {
      "User": $userOpenApiSchema
    },
    "securitySchemes": {
      "http": {
        "type": "HTTP",
        "scheme": "basic"
      }
    }
  }
}
""".formatJson()


@Language("JSON")
val crudExampleJson = """
{
  "openapi": "3.0.1",
  "info": {
    "title": "Example",
    "version": "1.0.0"
  },
  "paths": {
    "/users/{user-id}": {
      "get": {
        "summary": "Get users with userId",
        "operationId": "getUsersWithUserId",
        "parameters": [
          {
            "name": "user-id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "string"
            }
          }
        ],
        "responses": {
          "200": {
            "description": "",
            "content": {
              "application/json": {
                "schema": {
                  "$ref": "#/components/schemas/User"
                }
              }
            }
          }
        }
      },
      "delete": {
        "summary": "Delete users with userId",
        "operationId": "deleteUsersWithUserId",
        "parameters": [
          {
            "name": "user-id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "string"
            }
          }
        ]
      },
      "patch": {
        "summary": "Patch users with userId",
        "operationId": "patchUsersWithUserId",
        "parameters": [
          {
            "name": "user-id",
            "in": "path",
            "required": true,
            "schema": {
              "type": "string"
            }
          }
        ]
      }
    },
    "/users": {
      "get": {
        "summary": "Get users",
        "operationId": "getUsers",
        "responses": {
          "200": {
            "description": "",
            "content": {
              "application/json": {
                "schema": {
                  "type": "array",
                  "items": {
                    "$ref": "#/components/schemas/User"
                  }
                }
              }
            }
          }
        }
      },
      "post": {
        "summary": "Post users",
        "operationId": "postUsers"
      }
    }
  },
  "components": {
    "schemas": {
      "User": $userOpenApiSchema
    }
  }
}
""".formatJson()
