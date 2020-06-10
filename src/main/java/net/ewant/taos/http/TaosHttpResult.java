package net.ewant.taos.http;

import java.util.List;

public class TaosHttpResult {

    private static final String SUCCESS_STATUS = "succ";
    private static final String HEAD_ROWS = "affected_rows";

    private String status = SUCCESS_STATUS;
    private String[] head;
    private List<String[]> data;
    private int rows;

    public boolean ok(){
        return SUCCESS_STATUS.equals(status);
    }

    public boolean isAffectedRows(String head){
        return HEAD_ROWS.equalsIgnoreCase(head);
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String[] getHead() {
        return head;
    }

    public void setHead(String[] head) {
        this.head = head;
    }

    public List<String[]> getData() {
        return data;
    }

    public void setData(List<String[]> data) {
        this.data = data;
    }

    public int getRows() {
        return rows;
    }

    public void setRows(int rows) {
        this.rows = rows;
    }
}
