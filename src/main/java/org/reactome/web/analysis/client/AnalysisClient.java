package org.reactome.web.analysis.client;

import com.google.gwt.http.client.*;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import org.reactome.web.analysis.client.exceptions.AnalysisModelException;
import org.reactome.web.analysis.client.model.*;
import org.reactome.web.analysis.client.model.factory.AnalysisModelFactory;
import org.reactome.web.pwp.model.classes.Pathway;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

/**
 * @author Antonio Fabregat <fabregat@ebi.ac.uk>
 */
public abstract class AnalysisClient {

    public static String SERVER = "";
    private static final String ANALYSIS = "/AnalysisService";

    private static final Set<String> validTokens = new HashSet<>();

    public static Request analyseData(String data, boolean projection, int pageSize, int page, final AnalysisHandler.Result handler) {
        String url = SERVER + ANALYSIS + "/identifiers" + (projection ? "/projection" : "") + "?pageSize=" + pageSize + "&page=" + page;
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
        requestBuilder.setHeader("Content-Type", "text/plain");
        return analyse(requestBuilder, data, handler);
    }

    public static Request speciesComparison(Long dbId, int pageSize, int page, final AnalysisHandler.Result handler) {
        String url = SERVER + ANALYSIS + "/species/homoSapiens/" + dbId + "?pageSize=" + pageSize + "&page=" + page;
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
        return analyse(requestBuilder, null, handler);
    }

    private static Request analyse(RequestBuilder requestBuilder, String data, final AnalysisHandler.Result handler) {
        requestBuilder.setHeader("Accept", "application/json");
        try {
            final long start = System.currentTimeMillis();
            return requestBuilder.sendRequest(data, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    switch (response.getStatusCode()) {
                        case Response.SC_OK:
                            try {
                                AnalysisResult result = AnalysisModelFactory.getModelObject(AnalysisResult.class, response.getText());
                                long time = System.currentTimeMillis() - start;
                                handler.onAnalysisResult(result, time);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                            break;
                        default:
                            try {
                                AnalysisError analysisError = AnalysisModelFactory.getModelObject(AnalysisError.class, response.getText());
                                handler.onAnalysisError(analysisError);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    handler.onAnalysisServerException(exception.getMessage());
                }
            });
        } catch (RequestException ex) {
            handler.onAnalysisServerException(ex.getMessage());
        }
        return null;
    }

    public static void addValidToken(String token) {
        if (token != null && !token.isEmpty()) validTokens.add(token);
    }

    public static Request checkTokenAvailability(final String token, final AnalysisHandler.Token handler) {
        if (token == null || validTokens.contains(token)) { //YES, a null token is valid (it means there is not analysis overlay))
            handler.onTokenAvailabilityChecked(true, null);
        } else {
            String url = SERVER + ANALYSIS + "/token/" + token + "?pageSize=0&page=1";
            RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
            try {
                return requestBuilder.sendRequest(null, new RequestCallback() {
                    @Override
                    public void onResponseReceived(Request request, Response response) {
                        switch (response.getStatusCode()) {
                            case Response.SC_OK:
                                validTokens.add(token);
                                handler.onTokenAvailabilityChecked(true, null);
                                break;
                            case Response.SC_GONE:
                                handler.onTokenAvailabilityChecked(false, "Your result may have been deleted due to a new content release.\n" +
                                        "Please submit your data again to obtain results from the latest version of our database");
                                break;
                            default:
                                try {
                                    AnalysisError analysisError = AnalysisModelFactory.getModelObject(AnalysisError.class, response.getText());
                                    handler.onTokenAvailabilityChecked(false, analysisError.getReason());
                                } catch (AnalysisModelException e) {
                                    handler.onAnalysisServerException(e.getMessage());
                                }
                        }
                    }

                    @Override
                    public void onError(Request request, Throwable exception) {
                        handler.onTokenAvailabilityChecked(false, "An error happened while checking the analysis results availability");
                    }
                });
            } catch (RequestException ex) {
                handler.onAnalysisServerException(ex.getMessage());
            }
        }
        return null;
    }

    public static Request getResult(String token, String resource, int pageSize, int page, final AnalysisHandler.Result handler) {
        String url = SERVER + ANALYSIS + "/token/" + token + "?resource=" + resource + "&pageSize=" + pageSize + "&page=" + page;
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
        try {
            final long start = System.currentTimeMillis();
            return requestBuilder.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    long time;
                    switch (response.getStatusCode()) {
                        case Response.SC_OK:
                            try {
                                AnalysisResult result = AnalysisModelFactory.getModelObject(AnalysisResult.class, response.getText());
                                time = System.currentTimeMillis() - start;
                                handler.onAnalysisResult(result, time);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                            break;
                        default:
                            try {
                                AnalysisError analysisError = AnalysisModelFactory.getModelObject(AnalysisError.class, response.getText());
                                handler.onAnalysisError(analysisError);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                    }
                }

                @Override
                public void onError(Request request, Throwable throwable) {
                    handler.onAnalysisServerException(throwable.getMessage());
                }
            });
        } catch (RequestException e) {
            handler.onAnalysisServerException(e.getMessage());
        }
        return null;
    }

    public static Request getResultSummary(String token, String resource, final AnalysisHandler.Summary handler) {
        String url = SERVER + ANALYSIS + "/token/" + token + "?resource=" + resource + "&pageSize=0&page=1";
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
        try {
            final long start = System.currentTimeMillis();
            return requestBuilder.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    long time;
                    switch (response.getStatusCode()) {
                        case Response.SC_OK:
                            try {
                                AnalysisResult result = AnalysisModelFactory.getModelObject(AnalysisResult.class, response.getText());
                                AnalysisSummary summary = result.getSummary();
                                ExpressionSummary expressionSummary = result.getExpression();
                                time = System.currentTimeMillis() - start;
                                handler.onResultSummaryLoaded(summary, expressionSummary, time);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                            break;
                        case Response.SC_NOT_FOUND:
                            time = System.currentTimeMillis() - start;
                            handler.onResultSummaryNotFound(time);
                            break;
                        default:
                            try {
                                AnalysisError analysisError = AnalysisModelFactory.getModelObject(AnalysisError.class, response.getText());
                                handler.onResultSummaryError(analysisError);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                    }
                }

                @Override
                public void onError(Request request, Throwable throwable) {
                    handler.onAnalysisServerException(throwable.getMessage());
                }
            });
        } catch (RequestException e) {
            handler.onAnalysisServerException(e.getMessage());
        }
        return null;
    }

    public static Request getPathwayIdentifiers(String token, String resource, Long pathwayDbId, final AnalysisHandler.Identifiers handler) {
        String url = SERVER + ANALYSIS + "/token/" + token + "/summary/" + pathwayDbId + "?resource=" + resource;
        return getPathwayIdentifiers(url, handler);
    }

    public static Request getPathwayIdentifiers(String token, String resource, Long pathwayDbId, int pageSize, int page, final AnalysisHandler.Identifiers handler) {
        String url = SERVER + ANALYSIS + "/token/" + token + "/summary/" + pathwayDbId + "?resource=" + resource + "&pageSize=" + pageSize + "&page=" + page;
        return getPathwayIdentifiers(url, handler);
    }

    private static Request getPathwayIdentifiers(String url, final AnalysisHandler.Identifiers handler) {
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
        try {
            final long start = System.currentTimeMillis();
            return requestBuilder.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    long time;
                    switch (response.getStatusCode()) {
                        case Response.SC_OK:
                            try {
                                PathwayIdentifiers pathwayIdentifiers = AnalysisModelFactory.getModelObject(PathwayIdentifiers.class, response.getText());
                                time = System.currentTimeMillis() - start;
                                handler.onPathwayIdentifiersLoaded(pathwayIdentifiers, time);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                            break;
                        case Response.SC_NOT_FOUND:
                            time = System.currentTimeMillis() - start;
                            handler.onPathwayIdentifiersNotFound(time);
                            break;
                        default:
                            try {
                                AnalysisError analysisError = AnalysisModelFactory.getModelObject(AnalysisError.class, response.getText());
                                handler.onPathwayIdentifiersError(analysisError);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    handler.onAnalysisServerException(exception.getMessage());
                }
            });
        } catch (RequestException e) {
            handler.onAnalysisServerException(e.getMessage());
        }
        return null;
    }

    public static Request getNotFoundIdentifiers(String token, int pageSize, int page, final AnalysisHandler.NotFoundIdentifiers handler) {
        String url = SERVER + ANALYSIS + "/token/" + token + "/notFound?pageSize=" + pageSize + "&page=" + page;
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
        requestBuilder.setHeader("Accept", "application/json");
        try {
            return requestBuilder.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    switch (response.getStatusCode()) {
                        case Response.SC_OK:
                            try {
                                List<IdentifierSummary> notFound = new LinkedList<IdentifierSummary>();
                                JSONArray aux = JSONParser.parseStrict(response.getText()).isArray();
                                for (int i = 0; i < aux.size(); i++) {
                                    JSONObject obj = aux.get(i).isObject();
                                    notFound.add(AnalysisModelFactory.getModelObject(IdentifierSummary.class, obj.toString()));
                                }
                                handler.onNotFoundIdentifiersLoaded(notFound);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                            break;
                        default:
                            try {
                                AnalysisError analysisError = AnalysisModelFactory.getModelObject(AnalysisError.class, response.getText());
                                handler.onNotFoundIdentifiersError(analysisError);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    handler.onAnalysisServerException(exception.getMessage());
                }
            });
        } catch (RequestException ex) {
            handler.onAnalysisServerException(ex.getMessage());
        }
        return null;
    }


    public static Request getPathwaySummaries(String token, String resource, List<String> pathways, final AnalysisHandler.Summaries handler) {
        String url = SERVER + ANALYSIS + "/token/" + token + "/filter/pathways?resource=" + resource;
        StringBuilder postData = new StringBuilder();
        for (String pathway : pathways) {
            postData.append(pathway).append(",");
        }
        if (postData.length() > 0) postData.deleteCharAt(postData.length() - 1);

        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
        try {
            final long start = System.currentTimeMillis();
            return requestBuilder.sendRequest(postData.toString(), new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    long time;
                    switch (response.getStatusCode()) {
                        case Response.SC_OK:
                            try {
                                List<PathwaySummary> pathwaySummaries = AnalysisModelFactory.getPathwaySummaryList(response.getText());
                                time = System.currentTimeMillis() - start;
                                handler.onPathwaySummariesLoaded(pathwaySummaries, time);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                            break;
                        case Response.SC_NOT_FOUND:
                            time = System.currentTimeMillis() - start;
                            handler.onPathwaySummariesNotFound(time);
                            break;
                        default:
                            try {
                                AnalysisError analysisError = AnalysisModelFactory.getModelObject(AnalysisError.class, response.getText());
                                handler.onPathwaySummariesError(analysisError);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    handler.onAnalysisServerException(exception.getMessage());
                }
            });
        } catch (RequestException ex) {
            handler.onAnalysisServerException(ex.getMessage());
        }
        return null;
    }

    public static Request filterResultBySpecies(String token, String resource, Long species, final AnalysisHandler.Pathways handler) {
        String url = SERVER + ANALYSIS + "/token/" + token + "/filter/species/" + species + "?resource=" + resource;
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
        requestBuilder.setHeader("Accept", "application/json");
        try {
            return requestBuilder.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    switch (response.getStatusCode()) {
                        case Response.SC_OK:
                            try {
                                String json = response.getText();
                                SpeciesFilteredResult result = AnalysisModelFactory.getModelObject(SpeciesFilteredResult.class, json);
                                handler.onPathwaysSpeciesFiltered(result);
                            } catch (Exception ex) {
                                handler.onAnalysisServerException(ex.getMessage());
                            }
                            break;
                        default:
                            try {
                                AnalysisError analysisError = AnalysisModelFactory.getModelObject(AnalysisError.class, response.getText());
                                handler.onPathwaysSpeciesError(analysisError);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                    }

                }

                @Override
                public void onError(Request request, Throwable exception) {
                    handler.onAnalysisServerException(exception.getMessage());
                }
            });
        } catch (RequestException ex) {
            handler.onAnalysisServerException(ex.getMessage());
        }
        return null;
    }

    public static Request getHitReactions(String token, String resource, Set<Pathway> pathways, final AnalysisHandler.Reactions handler) {
        StringBuilder post = new StringBuilder();
        for (Pathway pathway : pathways) {
            post.append(pathway.getDbId()).append(",");
        }
        post.delete(post.length() - 1, post.length());

        String url = SERVER + ANALYSIS + "/token/" + token + "/reactions/pathways?resource=" + resource;
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.POST, url);
        try {
            return requestBuilder.sendRequest(post.toString(), new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    switch (response.getStatusCode()) {
                        case Response.SC_OK:
                            try {
                                JSONArray res = JSONParser.parseStrict(response.getText()).isArray();
                                Set<Long> hitReactions = new HashSet<>();
                                for (int i = 0; i < res.size(); i++) {
                                    hitReactions.add(Long.valueOf(res.get(i).toString()));
                                }
                                handler.onReactionsAnalysisDataRetrieved(hitReactions);
                            } catch (Exception ex) {
                                handler.onAnalysisServerException(ex.getMessage());
                            }
                            break;
                        default:
                            try {
                                AnalysisError analysisError = AnalysisModelFactory.getModelObject(AnalysisError.class, response.getText());
                                handler.onReactionsAnalysisError(analysisError);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                    }
                }

                @Override
                public void onError(Request request, Throwable exception) {
                    handler.onAnalysisServerException(exception.getMessage());
                }
            });

        } catch (RequestException ex) {
            handler.onAnalysisServerException(ex.getMessage());
        }
        return null;
    }

    public static void findPathwayPage(Long pathway, String token, String resource, final AnalysisHandler.Page handler) {
        String url = SERVER + ANALYSIS + "/token/" + token + "/page/" + pathway + "?resource=" + resource;
        RequestBuilder requestBuilder = new RequestBuilder(RequestBuilder.GET, url);
        requestBuilder.setHeader("Accept", "application/json");
        try {
            requestBuilder.sendRequest(null, new RequestCallback() {
                @Override
                public void onResponseReceived(Request request, Response response) {
                    switch (response.getStatusCode()) {
                        case Response.SC_OK:
                            int page = Integer.valueOf(response.getText());
                            handler.onPageFound(page);
                            break;
                        default:
                            try {
                                AnalysisError analysisError = AnalysisModelFactory.getModelObject(AnalysisError.class, response.getText());
                                handler.onPageError(analysisError);
                            } catch (AnalysisModelException e) {
                                handler.onAnalysisServerException(e.getMessage());
                            }
                    }

                }

                @Override
                public void onError(Request request, Throwable exception) {
                    handler.onAnalysisServerException(exception.getMessage());
                }
            });
        } catch (RequestException ex) {
            handler.onAnalysisServerException(ex.getMessage());
        }
    }
}

