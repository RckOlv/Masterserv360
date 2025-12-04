import { ComponentFixture, TestBed } from '@angular/core/testing';

import { CategoryChart } from './category-chart';

describe('CategoryChart', () => {
  let component: CategoryChart;
  let fixture: ComponentFixture<CategoryChart>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CategoryChart]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CategoryChart);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
