package components.cms.parser.workbook;

import components.cms.parser.ParserException;
import components.cms.parser.model.NavigationLevel;
import components.cms.parser.model.navigation.column.Breadcrumbs;
import components.cms.parser.model.navigation.column.Buttons;
import components.cms.parser.model.navigation.column.ControlListEntries;
import components.cms.parser.model.navigation.column.Decontrols;
import components.cms.parser.model.navigation.column.Definitions;
import components.cms.parser.model.navigation.column.Loops;
import components.cms.parser.model.navigation.column.NavigationExtras;
import components.cms.parser.model.navigation.column.Nesting;
import components.cms.parser.model.navigation.column.Notes;
import components.cms.parser.model.navigation.column.OnPageContent;
import components.cms.parser.model.navigation.column.Redirect;
import components.cms.parser.util.Utils;
import org.apache.commons.lang3.StringUtils;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.slf4j.LoggerFactory;

import java.util.*;

public class NavigationParser {

    private static final org.slf4j.Logger LOGGER = LoggerFactory.getLogger(NavigationParser.class);

    public static final Map<Integer, String> sheetIndices;
    static
    {
        sheetIndices = new HashMap<>();
        sheetIndices.put(1, "UK_MILITARY_LIST"); // Column 2
        sheetIndices.put(2, "DUAL_USE_LIST"); // Column 3
    }

    private static class RowIndices {
        static final int NAVIGATION_START = 3; // Row 4
    }

    private static class ColumnIndices {
        static class NavigationExtras {
            static final int DIV_LINE = Utils.columnToIndex("A");
        }

        static class Navigation {
            static final int START = Utils.columnToIndex("B");
            static final int END = Utils.columnToIndex("K");
        }

        static class OnPageContent {
            static final int TITLE = Utils.columnToIndex("L");
            static final int EXPLANATORY_NOTES = Utils.columnToIndex("M");
        }

        static class ControlListEntries {
            static final int RATING = Utils.columnToIndex("N");
            static final int PRIORITY = Utils.columnToIndex("O");
        }

        static class Buttons {
            static final int SELECT_ONE = Utils.columnToIndex("P");
            static final int SELECT_MANY = Utils.columnToIndex("Q");
        }

        static class Nesting {
            static final int ANY_OF = Utils.columnToIndex("R");
            static final int ALL_OF = Utils.columnToIndex("S");
        }

        static class Loops {
            static final int RELATED_CODES = Utils.columnToIndex("T");
            static final int JUMP_TO = Utils.columnToIndex("U");
            static final int DEFINING = Utils.columnToIndex("V");
            static final int DEFERRED = Utils.columnToIndex("W");
        }

        static class Breadcrumbs {
            static final int BREADCRUMB_TEXT = Utils.columnToIndex("X");
        }

        static class Decontrols {
            static final int CONTENT = Utils.columnToIndex("Y");
            static final int NOTE = Utils.columnToIndex("Z");
            static final int TITLE = Utils.columnToIndex("AA");
            static final int EXPLANATORY_NOTES = Utils.columnToIndex("AB");
        }

        static class Definitions {
            static final int LOCAL = Utils.columnToIndex("AC");
        }

        static class Notes {
            static final int NB = Utils.columnToIndex("AD");
            static final int NOTE = Utils.columnToIndex("AE");
            static final int SEE_ALSO = Utils.columnToIndex("AF");
            static final int TECHNICAL_NOTE = Utils.columnToIndex("AG");
        }

        static class Redirect {
            static final int TCFCF = Utils.columnToIndex("AH");
        }
    }

    public static List<NavigationLevel> parse(Workbook workbook) {
        List<NavigationLevel> navigationLevels = new ArrayList<>();

        // Loop through sheets
        for (Map.Entry<Integer, String> entry : sheetIndices.entrySet()) {
            Deque<NavigationLevel> navLevelStack = new ArrayDeque<>();

            // Create a base NavigationLevel that can hold all parsed in
            NavigationLevel rootNavigationLevel = new NavigationLevel("ROOT", "ROOT", -1, entry.getValue());

            navLevelStack.push(rootNavigationLevel);

            Sheet sheet = workbook.getSheetAt(entry.getKey());

            for (int rowIdx = RowIndices.NAVIGATION_START; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    continue;
                }

                for (int navColIdx = ColumnIndices.Navigation.START; navColIdx <= ColumnIndices.Navigation.END; navColIdx++) {
                    String navCellValue = Utils.getCellValueByRowAndIndex(row, navColIdx);
                    if (navCellValue == null) {
                        continue;
                    }

                    String navCellAddress = row.getCell(navColIdx).getAddress().formatAsString();

                    int currIndentLevel = navColIdx - ColumnIndices.Navigation.START;
                    int prevIndentLevel = navLevelStack.peek().getLevel();

                    NavigationLevel navigationLevel = new NavigationLevel(navCellAddress, navCellValue, currIndentLevel, entry.getValue());

                    try {
                        NavigationExtras navigationExtras = getNavigationExtras(row);
                        OnPageContent onPageContent = getOnPageContent(row);
                        ControlListEntries controlListEntries = getControlListEntries(row);
                        Buttons buttons = getButtons(row);
                        Nesting nesting = getNesting(row);
                        Loops loops = getLoops(row);
                        Breadcrumbs breadcrumbs = getBreadcrumbs(row);
                        Decontrols decontrols = getDecontrols(row);
                        Definitions definitions = getDefinitions(row);
                        Notes notes = getNotes(row);
                        Redirect redirect = getRedirect(row);
                        navigationLevel =
                                new NavigationLevel(
                                        navCellAddress,
                                        navCellValue,
                                        currIndentLevel,
                                        entry.getValue(),
                                        navigationExtras,
                                        onPageContent,
                                        controlListEntries,
                                        buttons,
                                        nesting,
                                        loops,
                                        breadcrumbs,
                                        decontrols,
                                        definitions,
                                        notes,
                                        redirect);
                    } catch (ParserException e) {
                        LOGGER.error("Error progressing nav cell: {}, {}", navCellAddress, e.getMessage());
                    }

                    if (currIndentLevel < prevIndentLevel) {
                        // level higher: current = ML1a, previous = ML1a1
                        while (navLevelStack.peek().getLevel() >= currIndentLevel) {
                            navLevelStack.pop();
                        }
                        navLevelStack.peek().addSubNavigationLevel(navigationLevel);
                        navLevelStack.push(navigationLevel);
                    } else if (currIndentLevel > prevIndentLevel) {
                        // level lower: current = ML1a1, previous = ML1
                        navLevelStack.peek().addSubNavigationLevel(navigationLevel);
                        navLevelStack.push(navigationLevel);
                    } else {
                        // level same: current = ML1b, previous = ML1a
                        navLevelStack.pop();
                        navLevelStack.peek().addSubNavigationLevel(navigationLevel);
                        navLevelStack.push(navigationLevel);
                    }

                    break;
                }
            }

            navigationLevels.add(rootNavigationLevel);
        }

        return navigationLevels;
    }

    private static NavigationExtras getNavigationExtras(Row row) {
        String divLineStr = Utils.getCellValueByRowAndIndex(row, ColumnIndices.NavigationExtras.DIV_LINE);
        boolean divLine = "X".equalsIgnoreCase(divLineStr);
        return new NavigationExtras(divLine);
    }

    private static OnPageContent getOnPageContent(Row row) {
        String title = Utils.getCellValueByRowAndIndex(row, ColumnIndices.OnPageContent.TITLE);
        String explanatoryNotes = Utils.getCellValueByRowAndIndex(row, ColumnIndices.OnPageContent.EXPLANATORY_NOTES);
        return new OnPageContent(title, explanatoryNotes);
    }

    private static ControlListEntries getControlListEntries(Row row) {
        String rating = Utils.getCellValueByRowAndIndex(row, ColumnIndices.ControlListEntries.RATING);
        String priorityStr = Utils.getCellValueByRowAndIndex(row, ColumnIndices.ControlListEntries.PRIORITY);
        Integer priority = priorityStr == null ? null : Integer.parseInt(priorityStr);
        return new ControlListEntries(rating, priority);
    }

    private static Buttons getButtons(Row row) {
        String selectOne = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Buttons.SELECT_ONE);
        String selectMany = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Buttons.SELECT_MANY);
        if (StringUtils.isNoneBlank(selectOne, selectMany)) {
            throw new ParserException(String.format("Invalid button column state, select one: %s select many: %s", selectOne, selectMany));
        } else {
            if ("X".equalsIgnoreCase(selectOne)) {
                return Buttons.SELECT_ONE;
            } else if ("X".equalsIgnoreCase(selectMany)) {
                return Buttons.SELECT_MANY;
            } else {
                return null;
            }
        }
    }

    private static Nesting getNesting(Row row) {
        String anyOf = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Nesting.ANY_OF);
        String allOf = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Nesting.ALL_OF);
        if (StringUtils.isNoneBlank(anyOf, allOf)) {
            throw new ParserException(String.format("Invalid nesting column state, any of: %s all of: %s", anyOf, allOf));
        } else {
            if ("X".equalsIgnoreCase(anyOf)) {
                return Nesting.ANY_OF;
            } else if ("X".equalsIgnoreCase(allOf)) {
                return Nesting.ALL_OF;
            } else {
                return null;
            }
        }
    }

    private static Loops getLoops(Row row) {
        String relatedCodes = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Loops.RELATED_CODES);
        String jumpTo = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Loops.JUMP_TO);
        String defining = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Loops.DEFINING);
        String deferred = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Loops.DEFERRED);
        return new Loops(relatedCodes, jumpTo, defining, deferred);
    }

    private static Breadcrumbs getBreadcrumbs(Row row) {
        String breadcrumbText = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Breadcrumbs.BREADCRUMB_TEXT);
        return new Breadcrumbs(breadcrumbText);
    }

    private static Decontrols getDecontrols(Row row) {
        String content = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Decontrols.CONTENT);
        String note = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Decontrols.NOTE);
        String title = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Decontrols.TITLE);
        String explanatoryNotes = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Decontrols.EXPLANATORY_NOTES);
        return new Decontrols(content, note, title, explanatoryNotes);
    }

    private static Definitions getDefinitions(Row row) {
        String local = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Definitions.LOCAL);
        return new Definitions(local);
    }

    private static Notes getNotes(Row row) {
        String nb = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Notes.NB);
        String note = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Notes.NOTE);
        String seeAlso = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Notes.SEE_ALSO);
        String techNote = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Notes.TECHNICAL_NOTE);
        return new Notes(nb, note, seeAlso, techNote);
    }

    private static Redirect getRedirect(Row row) {
        String tcfcf = Utils.getCellValueByRowAndIndex(row, ColumnIndices.Redirect.TCFCF);
        return new Redirect("X".equalsIgnoreCase(tcfcf));
    }
}
