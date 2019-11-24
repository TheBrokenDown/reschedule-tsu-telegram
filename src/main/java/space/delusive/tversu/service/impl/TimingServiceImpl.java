package space.delusive.tversu.service.impl;

import org.springframework.stereotype.Component;
import space.delusive.tversu.entity.Cell;
import space.delusive.tversu.entity.DayOfWeek;
import space.delusive.tversu.entity.User;
import space.delusive.tversu.entity.WeekSign;
import space.delusive.tversu.rest.CellRepository;
import space.delusive.tversu.service.FacultyService;
import space.delusive.tversu.service.TimingService;
import space.delusive.tversu.util.DateUtils;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TimingServiceImpl implements TimingService {
    private final CellRepository cellRepository;
    private final FacultyService facultyService;

    public TimingServiceImpl(CellRepository cellRepository, FacultyService facultyService) {
        this.cellRepository = cellRepository;
        this.facultyService = facultyService;
    }

    @Override
    public Optional<Cell> getCurrentLesson(User user) {
        return getTodayLessonsAsStream(user)
                .filter(cell -> DateUtils.compareTime(cell.getStart(), DateUtils.getCurrentTime()) != -1)
                .filter(cell -> DateUtils.compareTime(cell.getEnd(), DateUtils.getCurrentTime()) != 1)
                .findFirst();
    }

    @Override
    public Optional<Cell> getNextLesson(User user) {
        return getTodayLessonsAsStream(user)
                .filter(cell -> DateUtils.compareTime(cell.getStart(), DateUtils.getCurrentTime()) == -1)
                .min((o1, o2) -> DateUtils.compareTime(o2.getStart(), o1.getStart()));
    }

    @Override
    public List<Cell> getTodayLessons(User user) {
        return getTodayLessonsAsStream(user).collect(Collectors.toList());
    }

    @Override
    public List<Cell> getTomorrowOrMondayLessons(User user) {
        DayOfWeek targetDay = DateUtils.getCurrentDayOfWeek() == DayOfWeek.SATURDAY ?
                DayOfWeek.MONDAY : DateUtils.getCurrentDayOfWeek().next();
        return getLessonsOfDayAsStream(user, targetDay, targetDay == DayOfWeek.MONDAY).collect(Collectors.toList());
    }

    @Override
    public Map<DayOfWeek, List<Cell>> getRemainingLessonsOfWeek(User user) {
        Map<DayOfWeek, List<Cell>> remainingLessonsOfWeek = new LinkedHashMap<>();
        getLessonsOfWeekAsStream(user, false)
                .filter(cell -> !cell.getDayOfWeek().isBeforeOf(DateUtils.getCurrentDayOfWeek()))
                .forEach(cell -> {
                    if (!remainingLessonsOfWeek.containsKey(cell.getDayOfWeek())) {
                        remainingLessonsOfWeek.put(cell.getDayOfWeek(), new ArrayList<>());
                    }
                    remainingLessonsOfWeek.get(cell.getDayOfWeek()).add(cell);
                });
        return remainingLessonsOfWeek;
    }

    private Stream<Cell> getTodayLessonsAsStream(User user) {
        return getLessonsOfDayAsStream(user, DateUtils.getCurrentDayOfWeek(), false);
    }

    private Stream<Cell> getLessonsOfDayAsStream(User user, DayOfWeek day, boolean isNextWeek) {
        return getLessonsOfWeekAsStream(user, isNextWeek)
                .filter(cell -> cell.getDayOfWeek() == day);
    }

    private Stream<Cell> getLessonsOfWeekAsStream(User user, boolean isNextWeek) {
        List<Cell> cells = cellRepository.getCells(user.getFaculty(), user.getGroup());
        uniteSimilarCells(cells);
        WeekSign targetWeekSign = isNextWeek ? facultyService.getNextWeekSign(user.getFaculty()) : facultyService.getCurrentWeekSign(user.getFaculty());
        return cells.stream()
                .filter(cell -> cell.getWeekSign() == targetWeekSign || cell.getWeekSign() == WeekSign.ANY)
                .filter(cell -> cell.getSubgroup() == user.getSubgroup() || cell.getSubgroup() == 0);
    }

    // i think i need to simplify this one, but now i'm too lazy to do it :p
    private void uniteSimilarCells(List<Cell> cells) {
        List<Cell> unitedCells = new ArrayList<>();
        for (Cell cell : cells) {
            if (unitedCells.contains(cell)) continue;
            List<Cell> similarCells = getSimilarCells(cell, cells);
            if (!similarCells.isEmpty()) {
                unitedCells.addAll(similarCells);
                similarCells.forEach(cell1 -> cell.setTeacherName(cell.getTeacherName() + ", " + cell1.getTeacherName()));
            }
        }
        cells.removeAll(unitedCells);
    }

    private List<Cell> getSimilarCells(Cell cell, List<Cell> allCells) {
        List<Cell> similarCells = new ArrayList<>();
        allCells.forEach(oneCell -> {
                boolean isSimilar = cell.getStart().equals(oneCell.getStart()) &&
                        cell.getDayOfWeek() == oneCell.getDayOfWeek() &&
                        cell.getSubgroup() == oneCell.getSubgroup() &&
                        cell.getWeekSign() == oneCell.getWeekSign() &&
                        cell.getFullSubjectName().equals(oneCell.getFullSubjectName()) &&
                        cell != oneCell;
                if (isSimilar) similarCells.add(oneCell);
        });
        return similarCells;
    }
}
